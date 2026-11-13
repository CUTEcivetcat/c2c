package com.c2c.popup.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.c2c.common.constant.ApiPath;
import com.c2c.common.result.R;
import com.c2c.popup.entity.ActivityPopup;
import com.c2c.popup.service.ActivityPopupService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 活动弹窗管理端：列表 / 新增 / 编辑 / 上下架 / 删除。需管理员公告权限。
 */
@RestController
@RequiredArgsConstructor
@Tag(name = "活动弹窗管理", description = "活动弹窗维护（可绑定优惠券）")
public class AdminPopupController {

    private final ActivityPopupService popupService;

    @Operation(summary = "弹窗管理列表（含下架）")
    @GetMapping(ApiPath.POPUP_ADMIN_LIST)
    public R<Page<ActivityPopup>> list(@Parameter(description = "页码") @RequestParam(defaultValue = "1") int page,
                                       @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") int size,
                                       @Parameter(description = "标题关键字") @RequestParam(required = false) String keyword) {
        return R.ok(popupService.listAll(page, size, keyword));
    }

    @Operation(summary = "创建弹窗")
    @PostMapping(ApiPath.POPUP_ADMIN)
    public R<ActivityPopup> create(@RequestBody ActivityPopup popup,
                                   @Parameter(hidden = true) @RequestHeader("X-User-Id") Long adminId) {
        return R.ok(popupService.create(popup, adminId));
    }

    @Operation(summary = "编辑弹窗")
    @PutMapping(ApiPath.POPUP_ADMIN_ID)
    public R<ActivityPopup> update(@Parameter(description = "弹窗 ID") @PathVariable Long id,
                                    @RequestBody ActivityPopup popup) {
        return R.ok(popupService.update(id, popup));
    }

    @Operation(summary = "上架/下架弹窗", description = "body：{status: 1展示 0下架}")
    @PutMapping(ApiPath.POPUP_ADMIN_STATUS)
    public R<Void> changeStatus(@Parameter(description = "弹窗 ID") @PathVariable Long id,
                                @RequestBody Map<String, Integer> body) {
        popupService.changeStatus(id, body.get("status"));
        return R.ok();
    }

    @Operation(summary = "删除弹窗")
    @DeleteMapping(ApiPath.POPUP_ADMIN_ID)
    public R<Void> delete(@Parameter(description = "弹窗 ID") @PathVariable Long id) {
        popupService.delete(id);
        return R.ok();
    }
}
