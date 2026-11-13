package com.c2c.coupon.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.c2c.common.constant.ApiPath;
import com.c2c.common.result.R;
import com.c2c.coupon.service.CouponService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 优惠券用户端：可领列表游客可读，领取/我的券/抵扣试算均需登录。
 */
@RestController
@RequiredArgsConstructor
@Tag(name = "优惠券", description = "领券 / 我的券 / 抵扣试算")
public class CouponController {

    private final CouponService couponService;

    @Operation(summary = "可领取券列表")
    @GetMapping(ApiPath.COUPON_AVAILABLE)
    public R<Page<Map<String, Object>>> available(@Parameter(description = "页码") @RequestParam(defaultValue = "1") int page,
                                                  @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") int size,
                                                  @Parameter(description = "商品分类ID（可空，传则只看通用+该分类券）") @RequestParam(required = false) Long categoryId,
                                                  @Parameter(hidden = true) @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        return R.ok(couponService.available(userId, page, size, categoryId));
    }

    @Operation(summary = "领取优惠券", description = "并发扣库存，防超发与重复领取")
    @PostMapping(ApiPath.COUPON_CLAIM)
    public R<Map<String, Object>> claim(@Parameter(description = "券模板 ID") @PathVariable Long couponId,
                                        @Parameter(hidden = true) @RequestHeader("X-User-Id") Long userId) {
        couponService.claim(couponId, userId);
        return R.ok();
    }

    @Operation(summary = "我的优惠券", description = "可按状态筛选：0 未使用 / 1 已使用 / 2 已过期 / 3 已作废")
    @GetMapping(ApiPath.COUPON_MY)
    public R<List<Map<String, Object>>> my(@Parameter(description = "状态") @RequestParam(required = false) Integer status,
                                           @Parameter(hidden = true) @RequestHeader("X-User-Id") Long userId) {
        return R.ok(couponService.myCoupons(userId, status));
    }

    @Operation(summary = "抵扣试算", description = "body：userCouponId、orderAmount（商品价+运费，抵扣前）、categoryId（商品分类，可空）；返回抵扣额与应付金额")
    @PostMapping(ApiPath.COUPON_CALC)
    public R<Map<String, Object>> calc(@RequestBody Map<String, Object> body,
                                       @Parameter(hidden = true) @RequestHeader("X-User-Id") Long userId) {
        Long userCouponId = body.get("userCouponId") == null ? null : Long.valueOf(body.get("userCouponId").toString());
        BigDecimal orderAmount = new BigDecimal(body.get("orderAmount").toString());
        Long categoryId = body.get("categoryId") == null ? null : Long.valueOf(body.get("categoryId").toString());
        return R.ok(couponService.calculate(userCouponId, userId, orderAmount, categoryId));
    }
}
