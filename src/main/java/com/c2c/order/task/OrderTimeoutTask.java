package com.c2c.order.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.c2c.common.enums.ProductStatus;
import com.c2c.order.entity.Order;
import com.c2c.order.delegate.ProductDelegate;
import com.c2c.order.mapper.OrderMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 订单超时关单兜底任务。
 *
 * <p>原下单时的"订单超时"事件在单体模式下仅打日志、无消费者，待支付订单会永久滞留。
 * 此处用定时任务每 60 秒扫描一次：超过 30 分钟未支付的待支付订单自动取消并恢复商品在售。</p>
 *
 * <p>并发安全：与"用户恰好同时支付"竞争时，条件更新（status=0 才改）保证只有一方成功，
 * 支付成功则本次扫描跳过该订单，不会造成"已支付却被取消"。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderTimeoutTask {

    /** 待支付超时时间（与下单接口返回的 expireTime 一致） */
    private static final int TIMEOUT_MINUTES = 30;

    private final OrderMapper orderMapper;
    private final ProductDelegate productDelegate;

    @Scheduled(fixedDelay = 60_000, initialDelay = 60_000)
    @Transactional
    public void cancelTimeoutOrders() {
        LocalDateTime deadline = LocalDateTime.now().minusMinutes(TIMEOUT_MINUTES);
        List<Order> timeoutOrders = orderMapper.selectList(new LambdaQueryWrapper<Order>()
                .eq(Order::getStatus, 0)
                .lt(Order::getCreatedAt, deadline));
        if (timeoutOrders.isEmpty()) {
            return;
        }
        for (Order order : timeoutOrders) {
            int updated = orderMapper.update(null, new com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper<Order>()
                    .eq("id", order.getId())
                    .eq("status", 0)
                    .set("status", 5)
                    .set("cancel_time", LocalDateTime.now())
                    .set("cancel_reason", "超时未支付，系统自动取消"));
            if (updated == 0) {
                // 状态已被并发修改（如用户刚好完成支付），跳过
                continue;
            }
            productDelegate.updateStatus(order.getProductId(), ProductStatus.ON_SALE.getCode());
            log.info("timeout order cancelled: orderId={}, orderNo={}", order.getId(), order.getOrderNo());
        }
    }
}
