package com.c2c.order.mq;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
/**
 * 订单事件发布（MQ）：单体模式下订单事件不发送 MQ，仅通过日志记录，便于后续拆分为微服务。
 */
@Component
public class OrderEventPublisher {
    /** 发布“订单创建”事件（单体模式下仅打日志）。 */
    public void publishOrderCreated(Long orderId, String orderNo) {
        log.info("Order created event skipped in monolith mode: orderId={}, orderNo={}", orderId, orderNo);
    }

    /** 发布“订单超时”事件（单体模式下仅打日志）。 */
    public void publishOrderTimeout(Long orderId) {
        log.info("Order timeout event skipped in monolith mode: orderId={}", orderId);
    }

    /** 发布“订单支付成功”事件（单体模式下仅打日志）。 */
    public void publishOrderPaid(Long orderId) {
        log.info("Order paid event skipped in monolith mode: orderId={}", orderId);
    }
}

