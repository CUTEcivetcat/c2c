package com.c2c.ai.controller;

import com.c2c.ai.dto.AiChatRequest;
import com.c2c.ai.dto.AiChatVO;
import com.c2c.ai.service.AiChatService;
import com.c2c.common.constant.ApiPath;
import com.c2c.common.result.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;

/**
 * AI 智能客服接口（登录用户）。
 *
 * <p>对话发生在"用户 ↔ 平台助手(id=90000001)"的会话里：
 * 前端在助手会话内发消息走 {@code POST /ai/chat}（替代 /im/message），
 * 回复以 messageType=4 系统消息落库，聊天页灰条展示。</p>
 */
@RestController
@RequiredArgsConstructor
@Tag(name = "AI 智能客服", description = "平台助手对话：查订单/问规则/公告/余额；大模型优先，规则引擎降级")
public class AiChatController {

    private final AiChatService aiChatService;

    @Operation(summary = "助手对话", description = "body: {conversationId, content}；返回助手回复（已落库），source=model/rule")
    @PostMapping(ApiPath.AI_CHAT)
    public R<AiChatVO> chat(@Parameter(hidden = true) @RequestHeader("X-User-Id") Long userId,
                            @Valid @RequestBody AiChatRequest req) {
        return R.ok(aiChatService.chat(userId, req));
    }

    @Operation(summary = "快捷问题", description = "FAQ Top8（聊天页输入区上方 chips）")
    @GetMapping(ApiPath.AI_CHAT_FAQ)
    public R<List<String>> faqSuggestions() {
        return R.ok(aiChatService.faqSuggestions());
    }
}
