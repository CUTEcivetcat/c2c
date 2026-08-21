package com.c2c.admin.controller;

import com.c2c.admin.feign.OrderFeignClient;
import com.c2c.admin.feign.ProductFeignClient;
import com.c2c.admin.feign.UserFeignClient;
import com.c2c.admin.mapper.DashboardMapper;
import com.c2c.announcement.mapper.AnnouncementMapper;
import com.c2c.common.constant.ApiPath;
import com.c2c.common.result.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
    private final DashboardMapper dashboardMapper;
    private final AnnouncementMapper announcementMapper;

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
            "todayOrders", safeLong(orderData, "todayOrders"),
            "totalAnnouncements", announcementMapper.selectCount(null)
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
     * 7 日趋势数据（真实数据库查询：近 7 天新增用户 / 订单 / 交易额）
     */
    @Operation(summary = "7 日趋势数据", description = "近 7 日新增用户 / 订单 / 交易额（真实数据）")
    @GetMapping(ApiPath.ADMIN_DASHBOARD_TRENDS)
    public R<Map<String, Object>> trends() {
        LocalDate today = LocalDate.now();
        LocalDate sevenDaysAgo = today.minusDays(6);
        LocalDateTime since = LocalDateTime.of(sevenDaysAgo, LocalTime.MIN);

        // 按日期建立 Map 方便填充
        Map<String, int[]> dayMap = new java.util.LinkedHashMap<>();
        for (int i = 6; i >= 0; i--) {
            String key = today.minusDays(i).toString();
            dayMap.put(key, new int[]{0, 0, 0}); // {有数据标记, 订单数, 交易额}
        }
        // 查用户新增
        List<Map<String, Object>> userData = dashboardMapper.userTrend(since);
        for (Map<String, Object> row : userData) {
            String d = String.valueOf(row.get("d"));
            if (dayMap.containsKey(d)) {
                dayMap.get(d)[0] = toInt(row.get("cnt"));
            }
        }
        // 查订单趋势
        List<Map<String, Object>> orderData = dashboardMapper.orderTrend(since);
        for (Map<String, Object> row : orderData) {
            String d = String.valueOf(row.get("d"));
            if (dayMap.containsKey(d)) {
                dayMap.get(d)[1] = toInt(row.get("cnt"));
                dayMap.get(d)[2] = toInt(row.get("rev"));
            }
        }

        String[] dates = new String[7];
        int[] newUsers = new int[7];
        int[] orders = new int[7];
        double[] revenue = new double[7];
        int idx = 0;
        for (Map.Entry<String, int[]> e : dayMap.entrySet()) {
            dates[idx] = e.getKey().substring(5); // MM-DD
            newUsers[idx] = e.getValue()[0];
            orders[idx] = e.getValue()[1];
            revenue[idx] = e.getValue()[2];
            idx++;
        }

        return R.ok(com.c2c.common.utils.MapUtils.of("dates", dates, "newUsers", newUsers, "orders", orders, "revenue", revenue));
    }

    private int toInt(Object val) {
        if (val instanceof Number) return ((Number) val).intValue();
        try { return Integer.parseInt(String.valueOf(val)); } catch (Exception e) { return 0; }
    }
}
