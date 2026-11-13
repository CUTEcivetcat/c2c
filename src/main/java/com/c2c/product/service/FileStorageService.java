package com.c2c.product.service;

import com.c2c.common.exception.BusinessException;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * 文件存储服务
 * <p>负责商品图片等文件的上传、删除与访问 URL 生成，
 * 按日期(yyyy/MM/dd)分目录存储，文件名使用 UUID 保证唯一。
 * 上传前校验扩展名、MIME 与真实内容（ImageIO 解码），防止上传可执行/脚本文件。</p>
 */
@Slf4j
@Service
public class FileStorageService {

    /** 允许的图片扩展名 */
    private static final Set<String> ALLOWED_EXT = new HashSet<>(Arrays.asList(
            ".jpg", ".jpeg", ".png", ".gif", ".webp", ".bmp"));
    /** 允许的 MIME 类型（兜底校验） */
    private static final Set<String> ALLOWED_MIME = new HashSet<>(Arrays.asList(
            "image/jpeg", "image/png", "image/gif", "image/webp", "image/bmp"));
    /** 缩略图目标宽度（px） */
    private static final int THUMB_WIDTH = 400;
    /** 缩略图文件名后缀 */
    private static final String THUMB_SUFFIX = "_400.jpg";

    @Value("${app.upload-root}")
    private String uploadRoot;

    @Value("${app.upload-url-prefix:/files}")
    private String uploadUrlPrefix;

    /** 上传文件并返回原图 URL（兼容旧调用；缩略图随图一并生成） */
    public String upload(MultipartFile file) throws Exception {
        return uploadWithThumb(file).getUrl();
    }

    /**
     * 上传文件并同时生成 400px 缩略图，返回原图与缩略图地址。
     */
    public UploadedImage uploadWithThumb(MultipartFile file) throws Exception {
        validateFile(file);
        String datePath = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        String ext = getExtension(file.getOriginalFilename());
        String baseName = UUID.randomUUID().toString().replace("-", "");
        String filename = baseName + ext;

        File dir = new File(uploadRoot, datePath);
        if (!dir.exists() && !dir.mkdirs()) {
            throw new IllegalStateException("无法创建上传目录：" + dir.getAbsolutePath());
        }

        File target = new File(dir, filename);
        file.transferTo(target);

        String url = uploadUrlPrefix + "/" + datePath + "/" + filename;
        String thumbUrl = writeThumbnail(target, dir, datePath + "/", baseName);
        log.info("File uploaded: {} ({} bytes), thumb={}", url, file.getSize(), thumbUrl);
        return new UploadedImage(url, thumbUrl);
    }

    /**
     * 按原图地址兜底获取缩略图：已存在则复用，否则现场生成；失败返回 null（前端回退原图）。
     */
    public String ensureThumbForUrl(String originalUrl) {
        if (originalUrl == null || originalUrl.isBlank()) {
            return null;
        }
        String relativePath = originalUrl.replace(uploadUrlPrefix, "").replaceFirst("^/+", "");
        File original = new File(uploadRoot, relativePath);
        if (!original.exists()) {
            return null;
        }
        String fileName = original.getName();
        int dot = fileName.lastIndexOf('.');
        String baseName = dot > 0 ? fileName.substring(0, dot) : fileName;
        File dir = original.getParentFile();
        String dirRelative = relativePath.substring(0, relativePath.length() - fileName.length());
        File thumbFile = new File(dir, baseName + THUMB_SUFFIX);
        String thumbUrl = uploadUrlPrefix + "/" + dirRelative + baseName + THUMB_SUFFIX;
        if (thumbFile.exists()) {
            return thumbUrl;
        }
        String generated = writeThumbnail(original, dir, dirRelative, baseName);
        return generated;
    }

    /**
     * 按原图生成 400px 宽 JPEG 缩略图；原图宽度不足 400 或解码失败时复用原图 / 返回 null。
     */
    private String writeThumbnail(File original, File dir, String dirRelative, String baseName) {
        try {
            BufferedImage src = ImageIO.read(original);
            if (src == null) {
                return null;
            }
            if (src.getWidth() <= THUMB_WIDTH) {
                // 小图不缩放，缩略图直接复用原图
                return uploadUrlPrefix + "/" + dirRelative + original.getName();
            }
            int targetW = THUMB_WIDTH;
            int targetH = (int) (src.getHeight() * (targetW / (double) src.getWidth()));
            BufferedImage thumb = new BufferedImage(targetW, targetH, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = thumb.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, targetW, targetH);
            g.drawImage(src, 0, 0, targetW, targetH, null);
            g.dispose();
            String thumbName = baseName + THUMB_SUFFIX;
            File thumbFile = new File(dir, thumbName);
            ImageIO.write(thumb, "JPEG", thumbFile);
            return uploadUrlPrefix + "/" + dirRelative + thumbName;
        } catch (IOException e) {
            log.warn("缩略图生成失败: {}", original.getAbsolutePath(), e);
            return null;
        }
    }

    /**
     * 上传前校验：扩展名 + MIME + 真实图片内容（ImageIO 解码魔数）
     */
    private void validateFile(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("文件不能为空");
        }
        if (file.getSize() > 10 * 1024 * 1024) {
            throw new BusinessException("图片大小不能超过 10MB");
        }
        String ext = getExtension(file.getOriginalFilename());
        if (!ALLOWED_EXT.contains(ext)) {
            throw new BusinessException("仅支持上传 jpg/png/gif/webp/bmp 图片");
        }
        String contentType = file.getContentType();
        if (contentType != null && !ALLOWED_MIME.contains(contentType.toLowerCase())) {
            throw new BusinessException("非法的图片类型");
        }
        // 真实内容校验：ImageIO 能解码才算图片，拦截伪装成图片的脚本/可执行文件
        BufferedImage img;
        try {
            img = ImageIO.read(file.getInputStream());
        } catch (Exception e) {
            throw new BusinessException("图片内容解析失败");
        }
        if (img == null) {
            throw new BusinessException("不是有效的图片文件");
        }
    }

    /**
     * 根据对象名删除本地文件。
     * 校验规范化路径仍位于上传根目录内，拒绝绝对路径与 `..` 目录穿越。
     */
    public void delete(String objectName) {
        if (objectName == null || objectName.isBlank()) {
            return;
        }
        String relativePath = objectName.replace(uploadUrlPrefix, "").replaceFirst("^/+", "");
        if (relativePath.isBlank() || relativePath.contains("..") || new File(relativePath).isAbsolute()) {
            log.warn("拒绝删除非法文件路径: {}", objectName);
            return;
        }
        try {
            File root = new File(uploadRoot).getCanonicalFile();
            File target = new File(root, relativePath).getCanonicalFile();
            if (!target.getPath().startsWith(root.getPath() + File.separator)) {
                log.warn("删除路径越出上传目录: {}", target.getPath());
                return;
            }
            if (target.exists() && !target.delete()) {
                log.warn("Failed to delete file: {}", target.getAbsolutePath());
            }
        } catch (IOException e) {
            log.warn("文件路径解析失败: {}", objectName, e);
        }
    }

    /** 获取预签名访问 URL（本地存储直接返回原对象名） */
    public String getPresignedUrl(String objectName) {
        return objectName;
    }

    private String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return ".jpg";
        }
        return filename.substring(filename.lastIndexOf(".")).toLowerCase();
    }

    /** 上传结果：原图地址 + 400px 缩略图地址 */
    @Data
    public static class UploadedImage {

        /** 原图 URL */
        private final String url;

        /** 缩略图 URL（生成失败时为 null，前端回退原图） */
        private final String thumbUrl;

        public UploadedImage(String url, String thumbUrl) {
            this.url = url;
            this.thumbUrl = thumbUrl;
        }
    }
}
