package com.c2c.rating.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.c2c.common.constant.ApiPath;
import com.c2c.common.result.R;
import com.c2c.rating.entity.Rating;
import com.c2c.rating.service.RatingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 用户互评（交易完成后评价）：提交评价 / 某用户的评价分页 / 某订单的评价。提交需登录。
 */
@RestController
@RequiredArgsConstructor
@Tag(name = "用户评价", description = "交易后互评：提交评价 / 查看某用户评价 / 某订单评价")
public class RatingController {

    private final RatingService ratingService;

    @Operation(summary = "提交评价", description = "需登录，评价对象与订单写入请求体")
    @PostMapping(ApiPath.RATING)
    public R<Void> submit(@RequestBody Rating rating,
                          @Parameter(hidden = true) @RequestHeader("X-User-Id") Long userId) {
        ratingService.submit(rating, userId);
        return R.ok();
    }

    @Operation(summary = "某用户的评价（分页）", description = "公开接口")
    @GetMapping(ApiPath.RATING_USER_ID)
    public R<Page<Rating>> userRatings(@Parameter(description = "被评价用户 ID") @PathVariable Long userId,
                                       @Parameter(description = "页码，从 1 开始") @RequestParam(defaultValue = "1") int page,
                                       @Parameter(description = "每页条数") @RequestParam(defaultValue = "20") int size) {
        return R.ok(ratingService.getUserRatings(userId, page, size));
    }

    @Operation(summary = "某订单的评价")
    @GetMapping(ApiPath.RATING_ORDER_ID)
    public R<Map<String, Object>> orderRatings(@Parameter(description = "订单 ID") @PathVariable Long orderId) {
        return R.ok(ratingService.getOrderRatings(orderId));
    }
}
