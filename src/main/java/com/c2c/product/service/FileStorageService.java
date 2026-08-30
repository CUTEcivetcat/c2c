package com.c2c.product.service;

import com.c2c.common.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
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

    @Value("${app.upload-root}")
    private String uploadRoot;

    @Value("${app.upload-url-prefix:/files}")
    private String uploadUrlPrefix;

    /** 上传文件并返回可访问的 URL */
    public String upload(MultipartFile file) throws Exception {
        validateFile(file);
        String datePath = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        String ext = getExtension(file.getOriginalFilename());
        String filename = UUID.randomUUID().toString().replace("-", "") + ext;

        File dir = new File(uploadRoot, datePath);
        if (!dir.exists() && !dir.mkdirs()) {
            throw new IllegalStateException("无法创建上传目录：" + dir.getAbsolutePath());
        }

        File target = new File(dir, filename);
        file.transferTo(target);

        String url = uploadUrlPrefix + "/" + datePath + "/" + filename;
        log.info("File uploaded: {} ({} bytes)", url, file.getSize());
        return url;
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
        file.getInputStream().reset();
    }

    /** 根据对象名删除本地文件 */
    public void delete(String objectName) {
        if (objectName == null) {
            return;
        }
        String relativePath = objectName.replace(uploadUrlPrefix, "").replaceFirst("^/+", "");
        File target = new File(uploadRoot, relativePath);
        if (target.exists() && !target.delete()) {
            log.warn("Failed to delete file: {}", target.getAbsolutePath());
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
}
