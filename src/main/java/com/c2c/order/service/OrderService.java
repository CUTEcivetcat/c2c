package com.c2c.order.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.c2c.order.entity.Order;

import java.util.Map;

/**
 * 订单服务接口
 * <p>覆盖 C2C 交易订单的完整生命周期：下单、详情查询、列表查询、支付、发货、
 * 收货与取消等能力，买卖双方均有对应的操作权限控制。</p>
 */
public interface OrderService {

    /** 创建订单（校验商品与收货地址、锁定商品并发布订单事件） */
    Map<String, Object> create(Long productId, Long addressId, Long buyerId);

    /** 查询订单详情（买家或卖家均可查看） */
    Order getDetail(Long orderId, Long userId);

    /** 买家订单列表（可按状态筛选，分页） */
    Page<Order> getBuyerList(Long buyerId, Integer status, int page, int size);

    /** 卖家订单列表（可按状态筛选，分页） */
    Page<Order> getSellerList(Long sellerId, Integer status, int page, int size);

    /** 支付订单（模拟支付，并将商品置为已售状态） */
    void pay(Long orderId, Long buyerId);

    /** 卖家发货（填写物流公司与运单号） */
    void ship(Long orderId, Long sellerId, String shipCompany, String shipNo);

    /** 买家确认收货（订单自动进入完成状态） */
    void receive(Long orderId, Long buyerId);

    /** 取消订单（仅待支付订单可取消，并恢复商品为在售） */
    void cancel(Long orderId, Long userId, String reason);
}



