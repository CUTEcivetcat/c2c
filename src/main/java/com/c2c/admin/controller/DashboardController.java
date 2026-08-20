package com.c2c.admin.controller;

import com.c2c.admin.feign.OrderFeignClient;
import com.c2c.admin.feign.ProductFeignClient;
import com.c2c.admin.feign.UserFeignClient;
import com.c2c.common.constant.ApiPath;
import com.c2c.common.result.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 管理端首页看板：汇总用户 / 商品 / 订单概况，以及模拟的 7 日趋势数据。
 * 数据经 Feign 客户端聚合各服务统计接口而来。仅管理员可访问（role=ADMIN）。
 */
@RestController
@RequiredArgsConstructor
@Tag(name = "管理端看板", description = "用户/商品/订单统计汇总与趋势（仅管理员）")
public class DashboardController {

    private final UserFeignClient userFeignClient;
    private final ProductFeignClient productFeignClient;
    private final OrderFeignClient orderFeignClient;

    @Operation(summary = "首页统计汇总", description = "用户总数 / 商品总数 / 在售数 / 今日订单数")
    @GetMapping(ApiPath.ADMIN_DASHBOARD_SUMMARY)
    public R<Map<String, Object>> summary() {
        Map<String, Object> userCount = userFeignClient.countUsers();
        Map<String, Object> productCount = productFeignClient.countProducts();
        Map<String, Object> orderCount = orderFeignClient.countToday();

        Map<String, Object> userData = getData(userCount);
        Map<String, Object> productData = getData(productCount);
        Map<String, Object> orderData = getData(orderCount);

        return R.ok(com.c2c.common.utils.MapUtils.of(
            "totalUsers", safeLong(userData, "total"),
            "totalProducts", safeLong(productData, "total"),
            "onSaleProducts", safeLong(productData, "onSale"),
            "todayOrders", safeLong(orderData, "todayOrders")
        ));
    }

    @Operation(summary = "用户列表（管理端）", description = "关键字搜索 + 分页")
    @GetMapping(ApiPath.ADMIN_USERS)
    public R<Map<String, Object>> users(@Parameter(description = "搜索关键字") @RequestParam(required = false) String keyword,
                                        @Parameter(description = "页码，从 1 开始") @RequestParam(defaultValue = "1") int page,
                                        @Parameter(description = "每页条数") @RequestParam(defaultValue = "20") int size) {
        Map<String, Object> resp = userFeignClient.getUserList(keyword, page, size);
        return R.ok(getData(resp));
    }

    @Operation(summary = "启用/禁用用户（管理端）")
    @PutMapping(ApiPath.ADMIN_USER_STATUS)
    public R<Void> toggleUserStatus(@Parameter(description = "用户 ID") @PathVariable Long userId,
                                    @RequestBody Map<String, Integer> body) {
        userFeignClient.updateUserStatus(userId, body);
        return R.ok();
    }

    @Operation(summary = "商品列表（管理端）", description = "关键字 / 状态筛选 + 分页")
    @GetMapping(ApiPath.ADMIN_PRODUCTS)
    public R<Map<String, Object>> products(@Parameter(description = "标题关键字") @RequestParam(required = false) String keyword,
                                           @Parameter(description = "状态筛选") @RequestParam(required = false) Integer status,
                                           @Parameter(description = "页码，从 1 开始") @RequestParam(defaultValue = "1") int page,
                                           @Parameter(description = "每页条数") @RequestParam(defaultValue = "20") int size) {
        Map<String, Object> resp = productFeignClient.getProductList(keyword, status, page, size);
        return R.ok(getData(resp));
    }

    @Operation(summary = "上下架商品（管理端）")
    @PutMapping(ApiPath.ADMIN_PRODUCT_STATUS)
    public R<Void> toggleProductStatus(@Parameter(description = "商品 ID") @PathVariable Long id,
                                       @Parameter(description = "目标状态") @RequestParam Integer status) {
        productFeignClient.updateStatus(id, status);
        return R.ok();
    }

    @Operation(summary = "订单列表（管理端）", description = "状态筛选 + 分页")
    @GetMapping(ApiPath.ADMIN_ORDERS)
    public R<Map<String, Object>> orders(@Parameter(description = "状态筛选") @RequestParam(required = false) Integer status,
                                         @Parameter(description = "页码，从 1 开始") @RequestParam(defaultValue = "1") int page,
                                         @Parameter(description = "每页条数") @RequestParam(defaultValue = "20") int size) {
        Map<String, Object> resp = orderFeignClient.getOrderList(status, page, size);
        return R.ok(getData(resp));
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> getData(Map<String, Object> resp) {
        Object data = resp.get("data");
        if (data instanceof Map) return (Map<String, Object>) data;
        return com.c2c.common.utils.MapUtils.empty();
    }

    private long safeLong(Map<String, Object> map, String key) {
        Object val = map.get(key);
        if (val instanceof Number) return ((Number) val).longValue();
        return 0L;
    }

    /**
     * 模拟 7 日趋势数据（生产环境应查 DB）
     */
    @Operation(summary = "7 日趋势数据", description = "近 7 日新增用户 / 订单 / 营收（当前为模拟数据）")
    @GetMapping(ApiPath.ADMIN_DASHBOARD_TRENDS)
    public R<Map<String, Object>> trends() {
        String[] dates = new String[7];
        int[] newUsers = new int[7];
        int[] orders = new int[7];
        double[] revenue = new double[7];

        java.time.LocalDate today = java.time.LocalDate.now();
        java.util.Random rand = new java.util.Random();
        for (int i = 6; i >= 0; i--) {
            dates[6 - i] = today.minusDays(i).toString().substring(5); // 月-日 格式
            newUsers[6 - i] = rand.nextInt(10) + 1;
            orders[6 - i] = rand.nextInt(8) + 1;
            revenue[6 - i] = Math.round((rand.nextDouble() * 5000 + 100) * 100.0) / 100.0;
        }
        return R.ok(com.c2c.common.utils.MapUtils.of("dates", dates, "newUsers", newUsers, "orders", orders, "revenue", revenue));
    }
}
