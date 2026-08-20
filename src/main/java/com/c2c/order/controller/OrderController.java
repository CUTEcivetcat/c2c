package com.c2c.order.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.c2c.common.constant.ApiPath;
import com.c2c.common.result.R;
import com.c2c.order.entity.Order;
import com.c2c.order.mapper.OrderMapper;
import com.c2c.order.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

/**
 * 订单：创建 / 详情 / 买家列表 / 卖家列表 / 支付 / 发货 / 收货 / 取消，
 * 以及管理端的订单列表、今日订单数统计。均需登录；admin 开头接口需管理员 token。
 */
@RestController
@RequiredArgsConstructor
@Tag(name = "订单", description = "下单 / 支付 / 发货 / 收货 / 取消 / 买卖双方订单列表 / 管理端订单管理")
public class OrderController {

    private final OrderService orderService;
    private final OrderMapper orderMapper;

    @Operation(summary = "创建订单", description = "body 传 productId 与 addressId，需登录")
    @PostMapping(ApiPath.ORDER)
    public R<Map<String, Object>> create(@RequestBody Map<String, Long> body,
                                         @Parameter(hidden = true) @RequestHeader("X-User-Id") Long userId) {
        Map<String, Object> result = orderService.create(
                body.get("productId"), body.get("addressId"), userId);
        return R.ok(result);
    }

    @Operation(summary = "订单详情")
    @GetMapping(ApiPath.ORDER_ID)
    public R<Order> detail(@Parameter(description = "订单 ID") @PathVariable Long id,
                           @Parameter(hidden = true) @RequestHeader("X-User-Id") Long userId) {
        return R.ok(orderService.getDetail(id, userId));
    }

    @Operation(summary = "我买到的订单（买家列表）")
    @GetMapping(ApiPath.ORDER_LIST)
    public R<Page<Order>> buyerList(@Parameter(hidden = true) @RequestHeader("X-User-Id") Long userId,
                                    @Parameter(description = "状态筛选，不传查全部") @RequestParam(required = false) Integer status,
                                    @Parameter(description = "页码，从 1 开始") @RequestParam(defaultValue = "1") int page,
                                    @Parameter(description = "每页条数") @RequestParam(defaultValue = "20") int size) {
        return R.ok(orderService.getBuyerList(userId, status, page, size));
    }

    @Operation(summary = "我卖出的订单（卖家列表）")
    @GetMapping(ApiPath.ORDER_SELL_LIST)
    public R<Page<Order>> sellerList(@Parameter(hidden = true) @RequestHeader("X-User-Id") Long userId,
                                     @Parameter(description = "状态筛选，不传查全部") @RequestParam(required = false) Integer status,
                                     @Parameter(description = "页码，从 1 开始") @RequestParam(defaultValue = "1") int page,
                                     @Parameter(description = "每页条数") @RequestParam(defaultValue = "20") int size) {
        return R.ok(orderService.getSellerList(userId, status, page, size));
    }

    @Operation(summary = "支付订单")
    @PutMapping(ApiPath.ORDER_PAY)
    public R<Void> pay(@Parameter(description = "订单 ID") @PathVariable Long id,
                       @Parameter(hidden = true) @RequestHeader("X-User-Id") Long userId) {
        orderService.pay(id, userId);
        return R.ok();
    }

    @Operation(summary = "发货", description = "卖家操作，body 传 shipCompany / shipNo")
    @PutMapping(ApiPath.ORDER_SHIP)
    public R<Void> ship(@Parameter(description = "订单 ID") @PathVariable Long id,
                        @RequestBody Map<String, String> body,
                        @Parameter(hidden = true) @RequestHeader("X-User-Id") Long userId) {
        orderService.ship(id, userId, body.get("shipCompany"), body.get("shipNo"));
        return R.ok();
    }

    @Operation(summary = "确认收货")
    @PutMapping(ApiPath.ORDER_RECEIVE)
    public R<Void> receive(@Parameter(description = "订单 ID") @PathVariable Long id,
                           @Parameter(hidden = true) @RequestHeader("X-User-Id") Long userId) {
        orderService.receive(id, userId);
        return R.ok();
    }

    @Operation(summary = "取消订单", description = "body 可传 reason，默认“用户取消”")
    @PutMapping(ApiPath.ORDER_CANCEL)
    public R<Void> cancel(@Parameter(description = "订单 ID") @PathVariable Long id,
                          @RequestBody Map<String, String> body,
                          @Parameter(hidden = true) @RequestHeader("X-User-Id") Long userId) {
        orderService.cancel(id, userId, body.getOrDefault("reason", "用户取消"));
        return R.ok();
    }

    // ==================== Admin 接口 ====================

    @Operation(summary = "订单分页列表（管理端）")
    @GetMapping(ApiPath.ORDER_ADMIN_LIST)
    public R<Map<String, Object>> adminList(@Parameter(description = "状态筛选") @RequestParam(required = false) Integer status,
                                            @Parameter(description = "页码，从 1 开始") @RequestParam(defaultValue = "1") int page,
                                            @Parameter(description = "每页条数") @RequestParam(defaultValue = "20") int size) {
        LambdaQueryWrapper<Order> w = new LambdaQueryWrapper<>();
        if (status != null) w.eq(Order::getStatus, status);
        w.orderByDesc(Order::getCreatedAt);
        Page<Order> result = orderMapper.selectPage(new Page<>(page, size), w);
        return R.ok(com.c2c.common.utils.MapUtils.of("records", result.getRecords(), "total", result.getTotal(), "page", page, "size", size));
    }

    @Operation(summary = "今日订单数统计（管理端）")
    @GetMapping(ApiPath.ORDER_ADMIN_COUNT_TODAY)
    public R<Map<String, Long>> countToday() {
        java.time.LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        Long todayOrders = orderMapper.selectCount(
                new LambdaQueryWrapper<Order>().ge(Order::getCreatedAt, todayStart));
        return R.ok(com.c2c.common.utils.MapUtils.of("todayOrders", todayOrders));
    }
}
