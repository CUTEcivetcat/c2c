package com.c2c.admin.feign;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.c2c.order.entity.Order;
import com.c2c.order.mapper.OrderMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

/**
 * 调用订单模块的 Feign 客户端：供管理后台查询订单列表、统计今日下单数（单体模式下基于 Mapper 直接实现）。
 */
@Component("adminOrderFeignClient")
@RequiredArgsConstructor
public class OrderFeignClient {
    private final OrderMapper orderMapper;

    /** 分页查询订单列表，可按状态过滤，按创建时间倒序。 */
    public Map<String, Object> getOrderList(Integer status, int page, int size) {
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        if (status != null) {
            wrapper.eq(Order::getStatus, status);
        }
        wrapper.orderByDesc(Order::getCreatedAt);
        Page<Order> result = orderMapper.selectPage(new Page<Order>(page, size), wrapper);

        Map<String, Object> data = new HashMap<>();
        data.put("records", result.getRecords());
        data.put("total", result.getTotal());
        data.put("page", page);
        data.put("size", size);
        return ok(data);
    }

    /** 统计今日下单数。 */
    public Map<String, Object> countToday() {
        Map<String, Object> data = new HashMap<>();
        data.put("todayOrders", orderMapper.selectCount(
                new LambdaQueryWrapper<Order>().ge(Order::getCreatedAt, LocalDate.now().atStartOfDay())));
        return ok(data);
    }

    /** 构造统一成功响应。 */
    private Map<String, Object> ok(Object data) {
        Map<String, Object> map = new HashMap<>();
        map.put("code", 200);
        map.put("message", "success");
        map.put("data", data);
        return map;
    }
}

