package com.c2c.announcement.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.c2c.announcement.entity.Announcement;
import com.c2c.announcement.service.AnnouncementService;
import com.c2c.common.constant.ApiPath;
import com.c2c.common.result.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 公告（用户端公开）：公告列表与首页最新公告横幅，游客可访问（GET 只读）。
 */
@RestController
@RequiredArgsConstructor
@Tag(name = "平台公告", description = "用户端公告列表 / 首页最新公告横幅（公开只读）")
public class AnnouncementController {

    private final AnnouncementService announcementService;

    @Operation(summary = "公告列表（分页）", description = "仅返回已发布公告，置顶优先、时间倒序；可按类型筛选")
    @GetMapping(ApiPath.ANNOUNCEMENT_LIST)
    public R<Page<Announcement>> list(@Parameter(description = "页码") @RequestParam(defaultValue = "1") int page,
                                      @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") int size,
                                      @Parameter(description = "类型：1公告 2平台公约 3通知") @RequestParam(required = false) Integer type) {
        return R.ok(announcementService.listPublished(page, size, type));
    }

    @Operation(summary = "最新公告（首页横幅）", description = "返回最新若干条已发布公告，默认 3 条")
    @GetMapping(ApiPath.ANNOUNCEMENT_LATEST)
    public R<List<Announcement>> latest(@Parameter(description = "条数") @RequestParam(defaultValue = "3") int limit) {
        return R.ok(announcementService.listLatest(limit));
    }
}
