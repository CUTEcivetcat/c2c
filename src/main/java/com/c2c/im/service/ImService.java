package com.c2c.im.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.c2c.common.exception.BusinessException;
import com.c2c.im.entity.Conversation;
import com.c2c.im.entity.Message;
import com.c2c.im.mapper.ConversationMapper;
import com.c2c.im.mapper.MessageMapper;
import com.c2c.user.entity.User;
import com.c2c.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 即时通讯服务
 * <p>提供用户会话的获取/创建、会话列表、消息发送、消息分页查询、已读标记及未读统计能力。
 * 会话按小ID在前、大ID在后归一化存储，避免重复创建。</p>
 */
@Service
@RequiredArgsConstructor
public class ImService {

    private final ConversationMapper conversationMapper;
    private final MessageMapper messageMapper;
    private final UserMapper userMapper;

    /** 获取或创建两个用户之间的会话（可关联商品） */
    @Transactional
    public Conversation getOrCreateConversation(Long userId1, Long userId2, Long productId) {
        // 确保 user1Id < user2Id
        Long u1 = Math.min(userId1, userId2);
        Long u2 = Math.max(userId1, userId2);

        LambdaQueryWrapper<Conversation> w = new LambdaQueryWrapper<Conversation>()
                .eq(Conversation::getUser1Id, u1)
                .eq(Conversation::getUser2Id, u2);
        if (productId != null) {
            w.eq(Conversation::getProductId, productId);
        }
        Conversation conv = conversationMapper.selectOne(w);
        if (conv != null) return conv;

        conv = new Conversation();
        conv.setUser1Id(u1);
        conv.setUser2Id(u2);
        conv.setProductId(productId);
        conv.setUser1Unread(0);
        conv.setUser2Unread(0);
        conversationMapper.insert(conv);
        return conv;
    }

    /** 获取某用户参与的全部会话（按最后消息时间倒序） */
    public List<Conversation> getConversationList(Long userId) {
        return conversationMapper.selectList(new LambdaQueryWrapper<Conversation>()
                .and(w -> w.eq(Conversation::getUser1Id, userId).or().eq(Conversation::getUser2Id, userId))
                .orderByDesc(Conversation::getLastMessageTime));
    }

    /** 发送消息并更新会话的最后消息、时间与对方未读数 */
    @Transactional
    public Message sendMessage(Long conversationId, Long senderId, Long receiverId,
                                String content, Integer messageType, String extra) {
        Message msg = new Message();
        msg.setConversationId(conversationId);
        msg.setSenderId(senderId);
        msg.setReceiverId(receiverId);
        msg.setContent(content);
        msg.setMessageType(messageType != null ? messageType : 1);
        msg.setExtra(extra);
        msg.setIsRead(0);
        msg.setCreatedAt(LocalDateTime.now());
        messageMapper.insert(msg);

        // 更新会话
        Conversation conv = conversationMapper.selectById(conversationId);
        if (conv != null) {
            conv.setLastMessage(content.length() > 100 ? content.substring(0, 100) : content);
            conv.setLastMessageTime(msg.getCreatedAt());
            if (conv.getUser1Id().equals(receiverId)) {
                conv.setUser1Unread(conv.getUser1Unread() + 1);
            } else {
                conv.setUser2Unread(conv.getUser2Unread() + 1);
            }
            conversationMapper.updateById(conv);
        }

        return msg;
    }

    /** 分页拉取会话消息（支持按ID游标分页，结果按时间正序返回） */
    public Page<Message> getMessages(Long conversationId, Long before, Long after, int size) {
        LambdaQueryWrapper<Message> w = new LambdaQueryWrapper<Message>()
                .eq(Message::getConversationId, conversationId)
                .orderByDesc(Message::getCreatedAt);
        if (before != null && before > 0) {
            w.lt(Message::getId, before);
        }
        if (after != null && after > 0) {
            w.gt(Message::getId, after);
        }
        Page<Message> page = messageMapper.selectPage(new Page<>(1, size), w);
        // 反转顺序（前端上拉加载旧消息 / 轮询增量时用）
        java.util.Collections.reverse(page.getRecords());
        return page;
    }

    /**
     * 用户侧发消息：发送者必须属于该会话，接收者由会话推导（不信任前端传入），
     * 内容做非空与长度校验。校验通过后写入数据库并累计对方未读数。
     */
    @Transactional
    public Message sendMessageByUser(Long conversationId, Long senderId, String content) {
        Conversation conv = conversationMapper.selectById(conversationId);
        if (conv == null) {
            throw new BusinessException("会话不存在");
        }
        boolean senderInConv = conv.getUser1Id().equals(senderId) || conv.getUser2Id().equals(senderId);
        if (!senderInConv) {
            throw new BusinessException("无权在该会话中发言");
        }
        if (content == null || content.trim().isEmpty()) {
            throw new BusinessException("消息内容不能为空");
        }
        String trimmed = content.trim();
        if (trimmed.length() > 500) {
            throw new BusinessException("消息内容不能超过500字");
        }
        Long receiverId = conv.getUser1Id().equals(senderId) ? conv.getUser2Id() : conv.getUser1Id();
        return sendMessage(conversationId, senderId, receiverId, trimmed, 1, null);
    }

    /** 标记会话已读（清零未读数并将未读消息置为已读） */
    @Transactional
    public void markRead(Long conversationId, Long userId) {
        Conversation conv = conversationMapper.selectById(conversationId);
        if (conv != null) {
            if (conv.getUser1Id().equals(userId)) {
                conv.setUser1Unread(0);
            } else {
                conv.setUser2Unread(0);
            }
            conversationMapper.updateById(conv);
        }
        messageMapper.update(null, new LambdaUpdateWrapper<Message>()
                .eq(Message::getConversationId, conversationId)
                .eq(Message::getReceiverId, userId)
                .eq(Message::getIsRead, 0)
                .set(Message::getIsRead, 1));
    }

    /** 统计用户未读消息总数 */
    public int getUnreadCount(Long userId) {
        return messageMapper.selectCount(new LambdaQueryWrapper<Message>()
                .eq(Message::getReceiverId, userId)
                .eq(Message::getIsRead, 0)).intValue();
    }

    /**
     * 最新一条未读消息（供前端右上角弹窗提醒）
     * <p>无未读时仅返回 unreadTotal=0；有未读时附带发送人昵称、内容预览、会话 ID 等，
     * 前端据此展示"类似微信/QQ"的新消息通知。</p>
     */
    public Map<String, Object> getLatestUnread(Long userId) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("unreadTotal", getUnreadCount(userId));
        Message m = messageMapper.selectOne(new LambdaQueryWrapper<Message>()
                .eq(Message::getReceiverId, userId)
                .eq(Message::getIsRead, 0)
                .orderByDesc(Message::getId)
                .last("LIMIT 1"));
        if (m == null) {
            return result;
        }
        User sender = userMapper.selectById(m.getSenderId());
        String senderName = (sender != null && sender.getNickname() != null)
                ? sender.getNickname() : "用户" + m.getSenderId();
        result.put("messageId", m.getId());
        result.put("conversationId", m.getConversationId());
        result.put("senderId", m.getSenderId());
        result.put("senderName", senderName);
        result.put("content", m.getContent());
        result.put("time", m.getCreatedAt());
        return result;
    }
}



