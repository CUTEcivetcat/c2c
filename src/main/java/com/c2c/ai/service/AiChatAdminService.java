package com.c2c.ai.service;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.c2c.ai.dto.AiChatLogVO;
import com.c2c.im.entity.Message;
import com.c2c.im.mapper.MessageMapper;
import com.c2c.user.entity.User;
import com.c2c.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * AI 客服对话看板（管理端）：问答对抽样、来源分布统计、对话日志导出数据。
 *
 * <p>数据源为 IM 消息表：助手回复 messageType=4、senderId={@link AiChatService#ASSISTANT_ID}，
 * extra 含 {@code source}；对应用户提问为同会话 type=1 的用户消息。</p>
 */
@Service
@RequiredArgsConstructor
public class AiChatAdminService {

    /** 单次导出行数上限，避免日志过大 */
    private static final int EXPORT_LIMIT = 1000;

    private final MessageMapper messageMapper;
    private final UserMapper userMapper;

    /** 问答对分页抽样（按回复时间倒序） */
    public Page<AiChatLogVO> listLogs(int page, int size, String keyword, String source) {
        Page<Message> replyPage = messageMapper.selectPage(new Page<>(page, size),
                buildWrapper(keyword, source));
        Page<AiChatLogVO> result = new Page<>(replyPage.getCurrent(), replyPage.getSize(), replyPage.getTotal());
        result.setRecords(assemble(replyPage.getRecords()));
        return result;
    }

    /** 来源分布统计：total / model / rule */
    public Map<String, Object> stats() {
        long total = messageMapper.selectCount(assistantReplyWrapper(null));
        long model = messageMapper.selectCount(assistantReplyWrapper("model"));
        long rule = messageMapper.selectCount(assistantReplyWrapper("rule"));
        Map<String, Object> result = new HashMap<>();
        result.put("total", total);
        result.put("model", model);
        result.put("rule", rule);
        return result;
    }

    /** 全量对话日志（导出用，上限 {@link #EXPORT_LIMIT} 条） */
    public List<AiChatLogVO> exportLogs(String source) {
        List<Message> replies = messageMapper.selectList(buildWrapper(null, source)
                .last("LIMIT " + EXPORT_LIMIT));
        return assemble(replies);
    }

    // ==================== 组装 ====================

    /** 组装问答对：补同会话最近一条用户提问 + 提问用户昵称 */
    private List<AiChatLogVO> assemble(List<Message> replies) {
        List<AiChatLogVO> voList = new ArrayList<>();
        if (replies == null || replies.isEmpty()) return voList;

        // 会话下的用户消息，按会话分组（升序）
        Set<Long> convIds = replies.stream().map(Message::getConversationId).collect(Collectors.toSet());
        Map<Long, List<Message>> userMsgByConv = new HashMap<>();
        if (!convIds.isEmpty()) {
            List<Message> userMsgs = messageMapper.selectList(new LambdaQueryWrapper<Message>()
                    .in(Message::getConversationId, convIds)
                    .eq(Message::getMessageType, 1)
                    .ne(Message::getSenderId, AiChatService.ASSISTANT_ID)
                    .orderByAsc(Message::getId));
            for (Message m : userMsgs) {
                userMsgByConv.computeIfAbsent(m.getConversationId(), k -> new ArrayList<>()).add(m);
            }
        }

        // 提问用户昵称批量填充
        Set<Long> userIds = replies.stream().map(Message::getReceiverId).collect(Collectors.toSet());
        Map<Long, User> userMap = userIds.isEmpty() ? new HashMap<>()
                : userMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u));

        for (Message reply : replies) {
            AiChatLogVO vo = new AiChatLogVO();
            vo.setMessageId(reply.getId());
            vo.setConversationId(reply.getConversationId());
            vo.setUserId(reply.getReceiverId());
            User u = userMap.get(reply.getReceiverId());
            vo.setUserNickname(u != null && u.getNickname() != null ? u.getNickname() : "用户" + reply.getReceiverId());
            vo.setAnswer(reply.getContent());
            vo.setSource(parseSource(reply.getExtra()));
            vo.setCreatedAt(reply.getCreatedAt());
            vo.setQuestion(matchQuestion(userMsgByConv.get(reply.getConversationId()), reply));
            voList.add(vo);
        }
        return voList;
    }

    /** 在同会话用户消息中，取回复消息 ID 之前最近一条提问（ID 严格递增，避免同秒时间戳错配） */
    private String matchQuestion(List<Message> userMsgs, Message reply) {
        if (userMsgs == null || userMsgs.isEmpty()) return null;
        String question = null;
        for (Message m : userMsgs) {
            if (m.getId() != null && reply.getId() != null && m.getId() < reply.getId()) {
                question = m.getContent();
            }
        }
        return question;
    }

    private LambdaQueryWrapper<Message> buildWrapper(String keyword, String source) {
        LambdaQueryWrapper<Message> w = assistantReplyWrapper(source);
        if (StrUtil.isNotBlank(keyword)) {
            w.like(Message::getContent, keyword.trim());
        }
        w.orderByDesc(Message::getCreatedAt);
        return w;
    }

    /** 助手回复消息过滤：type=4 且发送者为助手；可按 source 过滤 */
    private LambdaQueryWrapper<Message> assistantReplyWrapper(String source) {
        LambdaQueryWrapper<Message> w = new LambdaQueryWrapper<Message>()
                .eq(Message::getMessageType, 4)
                .eq(Message::getSenderId, AiChatService.ASSISTANT_ID);
        if (StrUtil.isNotBlank(source)) {
            w.like(Message::getExtra, "\"source\":\"" + source + "\"");
        }
        return w;
    }

    /** 从 extra JSON 解析来源，缺失/异常归 rule */
    private String parseSource(String extra) {
        if (StrUtil.isBlank(extra)) return "rule";
        try {
            JSONObject obj = JSONUtil.parseObj(extra);
            String source = obj.getStr("source");
            return StrUtil.isBlank(source) ? "rule" : source;
        } catch (Exception e) {
            return "rule";
        }
    }
}
