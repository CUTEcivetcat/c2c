package com.c2c.product.controller;

import com.c2c.common.constant.ApiPath;
import com.c2c.common.result.R;
import com.c2c.product.service.FileStorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;

/**
 * 文件上传：单张 / 批量上传图片到服务器本地存储，返回可访问 URL。需登录。
 */
/**
 * 文件上传：单张 / 批量上传图片到服务器本地存储，返回可访问 URL。需登录。
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@Tag(name = "文件上传", description = "单张 / 批量上传图片，返回访问 URL（需登录）")
public class UploadController {

    private final FileStorageService storageService;

    /**
     * 上传单张图片 → 本地存储
     */
    @Operation(summary = "上传单张图片")
    @PostMapping(ApiPath.UPLOAD_IMAGE)
    public R<Map<String, String>> uploadImage(@Parameter(description = "图片文件（multipart）") @RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) return R.badRequest("文件不能为空");
        try {
            String url = storageService.upload(file);
            Map<String, String> result = new HashMap<>();
            result.put("url", url);
            return R.ok(result);
        } catch (Exception e) {
            log.error("上传失败", e);
            return R.fail("上传失败: " + e.getMessage());
        }
    }

    /**
     * 批量上传
     */
    @Operation(summary = "批量上传图片", description = "逐张上传，单张失败不影响其余，返回成功 URL 列表")
    @PostMapping(ApiPath.UPLOAD_IMAGES)
    public R<List<String>> uploadImages(@Parameter(description = "图片文件数组（multipart）") @RequestParam("files") List<MultipartFile> files) {
        List<String> urls = new ArrayList<>();
        for (MultipartFile file : files) {
            try {
                urls.add(storageService.upload(file));
            } catch (Exception e) {
                log.warn("文件上传失败: {}", file.getOriginalFilename());
            }
        }
        return R.ok(urls);
    }
}
