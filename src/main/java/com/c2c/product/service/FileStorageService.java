package com.c2c.product.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * 文件存储服务
 * <p>负责商品图片等文件的上传、删除与访问 URL 生成，
 * 按日期(yyyy/MM/dd)分目录存储，文件名使用 UUID 保证唯一。</p>
 */
@Slf4j
@Service
public class FileStorageService {

    @Value("${app.upload-root}")
    private String uploadRoot;

    @Value("${app.upload-url-prefix:/files}")
    private String uploadUrlPrefix;

    /** 上传文件并返回可访问的 URL */
    public String upload(MultipartFile file) throws Exception {
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

