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
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 公告管理（管理员）：发布 / 编辑 / 下架 / 删除 / 全量列表。需管理员 token。
 */
@RestController
@RequiredArgsConstructor
@Tag(name = "公告管理", description = "管理员发布与管理平台公告（需管理员权限）")
public class AdminAnnouncementController {

    private final AnnouncementService announcementService;

    @Operation(summary = "公告管理列表（含已下架）")
    @GetMapping(ApiPath.ANNOUNCEMENT_ADMIN_LIST)
    public R<Page<Announcement>> list(@Parameter(description = "页码") @RequestParam(defaultValue = "1") int page,
                                      @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") int size,
                                      @Parameter(description = "标题关键字") @RequestParam(required = false) String keyword) {
        return R.ok(announcementService.listAll(page, size, keyword));
    }

    @Operation(summary = "发布公告", description = "body：title/content/type(1公告 2公约 3通知)/pinned(0|1)")
    @PostMapping(ApiPath.ANNOUNCEMENT_ADMIN)
    public R<Announcement> create(@Parameter(hidden = true) @RequestHeader("X-User-Id") Long adminId,
                                  @RequestBody Announcement announcement) {
        return R.ok(announcementService.create(announcement, adminId));
    }

    @Operation(summary = "编辑公告")
    @PutMapping(ApiPath.ANNOUNCEMENT_ADMIN_ID)
    public R<Announcement> update(@Parameter(description = "公告 ID") @PathVariable Long id,
                                  @RequestBody Announcement announcement) {
        return R.ok(announcementService.update(id, announcement));
    }

    @Operation(summary = "下架/发布公告", description = "body：{status: 1发布 0下架}")
    @PutMapping(ApiPath.ANNOUNCEMENT_ADMIN_STATUS)
    public R<Void> changeStatus(@Parameter(description = "公告 ID") @PathVariable Long id,
                                @RequestBody Map<String, Integer> body) {
        announcementService.changeStatus(id, body.get("status"));
        return R.ok();
    }

    @Operation(summary = "删除公告")
    @DeleteMapping(ApiPath.ANNOUNCEMENT_ADMIN_ID)
    public R<Void> delete(@Parameter(description = "公告 ID") @PathVariable Long id) {
        announcementService.delete(id);
        return R.ok();
    }
}
