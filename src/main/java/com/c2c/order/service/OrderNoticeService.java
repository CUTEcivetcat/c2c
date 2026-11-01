package com.c2c.order.service;

import com.c2c.im.entity.Conversation;
import com.c2c.im.service.ImService;
import com.c2c.order.entity.Order;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 订单事件系统通知：状态流转时给买卖双方私信会话发 messageType=4 的系统消息。
 * 失败仅记日志，绝不阻断主交易流程（对齐 ReviewServiceImpl#notifyUser 的容错约定）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderNoticeService {

    private final ImService imService;

    /** 给买卖双方各发一条系统通知（双方视角的"对方"互为发送者，灰条展示无头像差异） */
    public void notifyOrderEvent(Order order, String eventText) {
        if (order == null || order.getBuyerId() == null || order.getSellerId() == null) {
            return;
        }
        String content = eventText + " #" + order.getOrderNo();
        // 传 productId：通知落进该商品的买卖会话，与"联系卖家"咨询同一会话
        sendTo(order.getSellerId(), order.getBuyerId(), order.getProductId(), content); // 买家收到的
        sendTo(order.getBuyerId(), order.getSellerId(), order.getProductId(), content); // 卖家收到的
    }

    private void sendTo(Long fromUserId, Long toUserId, Long productId, String content) {
        try {
            Conversation conv = imService.getOrCreateConversation(fromUserId, toUserId, productId);
            imService.sendMessage(conv.getId(), fromUserId, toUserId, content, 4, null);
        } catch (Exception e) {
            log.warn("订单通知发送失败: from={}, to={}, err={}", fromUserId, toUserId, e.getMessage());
        }
    }
}