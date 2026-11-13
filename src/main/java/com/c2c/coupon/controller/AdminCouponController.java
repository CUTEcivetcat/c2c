package com.c2c.coupon.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.c2c.common.constant.ApiPath;
import com.c2c.common.result.R;
import com.c2c.coupon.entity.CouponTemplate;
import com.c2c.coupon.service.CouponAdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 优惠券管理端：券模板列表 / 新增 / 编辑 / 启停 / 删除。需管理员 coupon 权限。
 */
@RestController
@RequiredArgsConstructor
@Tag(name = "优惠券管理", description = "优惠券发放批次维护（需管理员权限）")
public class AdminCouponController {

    private final CouponAdminService couponAdminService;

    @Operation(summary = "券模板管理列表（含停用）")
    @GetMapping(ApiPath.ADMIN_COUPON_LIST)
    public R<Page<CouponTemplate>> list(@Parameter(description = "页码") @RequestParam(defaultValue = "1") int page,
                                        @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") int size,
                                        @Parameter(description = "券名称关键字") @RequestParam(required = false) String keyword) {
        return R.ok(couponAdminService.listAll(page, size, keyword));
    }

    @Operation(summary = "创建券模板")
    @PostMapping(ApiPath.ADMIN_COUPON)
    public R<CouponTemplate> create(@RequestBody CouponTemplate template) {
        return R.ok(couponAdminService.create(template));
    }

    @Operation(summary = "编辑券模板")
    @PutMapping(ApiPath.ADMIN_COUPON_ID)
    public R<CouponTemplate> update(@Parameter(description = "券模板 ID") @PathVariable Long id,
                                    @RequestBody CouponTemplate template) {
        return R.ok(couponAdminService.update(id, template));
    }

    @Operation(summary = "启用/停用券", description = "body：{status: 1启用 0停用}")
    @PutMapping(ApiPath.ADMIN_COUPON_STATUS)
    public R<Void> changeStatus(@Parameter(description = "券模板 ID") @PathVariable Long id,
                                @RequestBody Map<String, Integer> body) {
        couponAdminService.changeStatus(id, body.get("status"));
        return R.ok();
    }

    @Operation(summary = "删除券模板")
    @DeleteMapping(ApiPath.ADMIN_COUPON_ID)
    public R<Void> delete(@Parameter(description = "券模板 ID") @PathVariable Long id) {
        couponAdminService.delete(id);
        return R.ok();
    }

    @Operation(summary = "定向分发余额券", description = "body: {userId}；直接入账到指定用户，无需领取")
    @PostMapping(ApiPath.ADMIN_COUPON_GRANT)
    public R<Void> grant(@Parameter(description = "券模板 ID") @PathVariable Long id,
                         @RequestBody Map<String, Object> body) {
        Long userId = body.get("userId") == null ? null : Long.valueOf(body.get("userId").toString());
        couponAdminService.grantToUser(id, userId);
        return R.ok();
    }
}
