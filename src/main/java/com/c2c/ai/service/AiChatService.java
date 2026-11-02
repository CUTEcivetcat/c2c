package com.c2c.ai.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.c2c.ai.client.AiOpenAiClient;
import com.c2c.ai.dto.AiChatRequest;
import com.c2c.ai.dto.AiChatVO;
import com.c2c.ai.entity.Faq;
import com.c2c.common.exception.BusinessException;
import com.c2c.im.entity.Conversation;
import com.c2c.im.entity.Message;
import com.c2c.im.mapper.ConversationMapper;
import com.c2c.im.service.ImService;
import com.c2c.system.constant.SysConfigKey;
import com.c2c.system.service.SysConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * AI 智能客服核心链路（平台助手会话）。
 *
 * <p>流程：开关/限频校验 → 会话归属校验（对端必须是助手账号）→ 落用户消息 →
 * FAQ 检索注入 → 大模型 + Function Calling（最多3轮工具循环）→
 * 失败/未配 Key 走规则引擎（FAQ 直答 / 工具正则直查）→ 助手消息落库（messageType=4，extra 标记来源）。</p>
 *
 * <p>三层降级：大模型+工具 → 规则引擎（FAQ/工具直查）→ 引导语；任何一层异常不影响主流程。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiChatService {

    /** 平台助手账号（user.id 固定，data.sql 种子） */
    public static final long ASSISTANT_ID = 90000001L;

    private static final String SYSTEM_PROMPT =
            "你是「青禾集市」二手交易平台的官方智能助手。规则：\n"
            + "1. 你只能回答本平台相关问题；回答事实性问题优先使用工具返回的数据，禁止编造。\n"
            + "2. 你只能访问当前登录用户本人的订单/余额数据（工具已做权限收敛）。\n"
            + "3. 以下平台规则可直接引用：\n"
            + "- 下单后30分钟内支付，超时自动取消；已支付订单卖家未发货前可取消并全额退款。\n"
            + "- 支付资金由平台托管，确认收货后打款卖家。\n"
            + "- 商品违规会被下架，可在「我的整改申诉」提交申诉。\n"
            + "4. 用户要求人工/投诉时调用 handoff_to_human 工具。回答控制在150字内，语气友好口语化。";

    private final ImService imService;
    private final ConversationMapper conversationMapper;
    private final SysConfigService sysConfigService;
    private final StringRedisTemplate redisTemplate;
    private final FaqSearchService faqSearchService;
    private final AiChatToolExecutor toolExecutor;
    private final AiOpenAiClient aiClient;

    /** 单轮问答：校验→落库→模型/规则→回复落库 */
    public AiChatVO chat(Long userId, AiChatRequest req) {
        checkEnabled();
        checkDailyLimit(userId);
        Conversation conv = checkAssistantConversation(req.getConversationId(), userId);

        // 1. 用户消息落库（复用 IM，自动更新会话与未读）
        imService.sendMessage(conv.getId(), userId, ASSISTANT_ID, req.getContent(), 1, null);

        // 2. 生成回复（模型优先，规则兜底）
        String reply;
        String source;
        try {
            String modelReply = tryModelReply(conv, userId, req.getContent());
            if (modelReply != null) {
                reply = modelReply;
                source = "model";
            } else {
                reply = ruleReply(userId, req.getContent());
                source = "rule";
            }
        } catch (Exception e) {
            log.warn("AI 客服生成失败，走规则兜底: {}", e.getMessage());
            reply = ruleReply(userId, req.getContent());
            source = "rule";
        }

        // 3. 助手回复落库（messageType=4 系统灰条；extra 标记 AI 来源）
        Message saved = imService.sendMessage(conv.getId(), ASSISTANT_ID, userId, reply, 4,
                "{\"ai\":true,\"source\":\"" + source + "\"}");
        return AiChatVO.builder()
                .messageId(saved == null ? null : saved.getId())
                .content(reply)
                .source(source)
                .build();
    }

    /** 快捷问题（聊天页 chips） */
    public List<String> faqSuggestions() {
        return faqSearchService.suggestions(8);
    }

    // ==================== 校验 ====================

    private void checkEnabled() {
        boolean enabled = sysConfigService.getBool(SysConfigKey.AI_ASSISTANT_ENABLED, true);
        if (!enabled) {
            throw new BusinessException("平台助手维护中，请稍后再试");
        }
    }

    private void checkDailyLimit(Long userId) {
        int limit = sysConfigService.getInt(SysConfigKey.AI_ASSISTANT_DAILY_LIMIT, 30);
        String key = "ai:chat:rate:" + userId + ":" + LocalDate.now();
        Long count = redisTemplate.opsForValue().increment(key);
        if (count != null && count == 1L) {
            redisTemplate.expire(key, 25, TimeUnit.HOURS);
        }
        if (count != null && count > limit) {
            throw new BusinessException("今日助手咨询次数已达上限，可前往平台公告查看常见规则");
        }
    }

    private Conversation checkAssistantConversation(Long conversationId, Long userId) {
        Conversation conv = conversationMapper.selectById(conversationId);
        if (conv == null) {
            throw new BusinessException("会话不存在");
        }
        boolean isMember = conv.getUser1Id().equals(userId) || conv.getUser2Id().equals(userId);
        boolean withAssistant = conv.getUser1Id().equals(ASSISTANT_ID) || conv.getUser2Id().equals(ASSISTANT_ID);
        if (!isMember || !withAssistant) {
            throw new BusinessException("该会话不是平台助手会话");
        }
        return conv;
    }

    // ==================== 大模型（工具调用最多3轮） ====================

    private String tryModelReply(Conversation conv, Long userId, String userText) {
        cn.hutool.json.JSONArray faqHits = new cn.hutool.json.JSONArray();
        faqSearchService.search(userText, 3).forEach(f -> faqHits.add(f.getQuestion() + "：" + f.getAnswer()));

        cn.hutool.json.JSONArray messages = new cn.hutool.json.JSONArray();
        messages.add(new cn.hutool.json.JSONObject().set("role", "system").set("content", SYSTEM_PROMPT
                + (faqHits.isEmpty() ? "" : "\n\n相关平台规则（可引用）：\n" + String.join("\n", faqHits.stream().map(Object::toString).toArray(String[]::new)))));

        // 上下文：本会话最近 10 条（含刚写入的用户消息）
        List<Message> recent = imService.getMessages(conv.getId(), userId, null, null, 10).getRecords();
        for (Message m : recent) {
            boolean fromUser = m.getSenderId().equals(userId);
            messages.add(new cn.hutool.json.JSONObject()
                    .set("role", fromUser ? "user" : "assistant")
                    .set("content", m.getContent()));
        }

        String tools = toolExecutor.toolsJson();
        for (int round = 0; round < 3; round++) {
            cn.hutool.json.JSONObject assistantMsg = aiClient.chat(messages, tools);
            if (assistantMsg == null) {
                return null; // 未配置/调用失败 → 规则兜底
            }
            String content = assistantMsg.getStr("content");
            cn.hutool.json.JSONArray toolCalls = assistantMsg.getJSONArray("tool_calls");
            if (toolCalls == null || toolCalls.isEmpty()) {
                return content;
            }
            // 有工具调用：执行并回填，继续下一轮
            messages.add(assistantMsg);
            for (Object tc : toolCalls) {
                cn.hutool.json.JSONObject call = (cn.hutool.json.JSONObject) tc;
                cn.hutool.json.JSONObject fn = call.getJSONObject("function");
                String result = toolExecutor.execute(fn.getStr("name"), fn.getStr("arguments"), userId);
                messages.add(new cn.hutool.json.JSONObject()
                        .set("role", "tool")
                        .set("tool_call_id", call.getStr("id"))
                        .set("content", result));
            }
        }
        cn.hutool.json.JSONObject finalMsg = aiClient.chat(messages, null);
        return finalMsg == null ? null : finalMsg.getStr("content");
    }

    // ==================== 规则引擎兜底 ====================

    private String ruleReply(Long userId, String userText) {
        // 1) FAQ 命中直答
        Faq hit = faqSearchService.bestMatch(userText);
        if (hit != null) {
            return hit.getAnswer();
        }
        // 2) 工具正则直查（订单号/余额/公告）
        String direct = toolExecutor.directQueryFromText(userText, userId);
        if (direct != null) {
            return direct;
        }
        // 3) 引导语
        return "我还不太理解你的问题～可以试试问我：\n· 我的订单到哪了\n· 余额还有多少\n· 下单后多久付款\n· 如何退款\n也可以点下方常见问题直接查看。";
    }
}
