package com.c2c.ai.dto;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * AI 客服对话请求。
 */
@Data
public class AiChatRequest {

    /** 助手会话 ID（对端必须是平台助手账号） */
    @NotNull(message = "会话ID不能为空")
    private Long conversationId;

    /** 用户输入 */
    @NotBlank(message = "内容不能为空")
    @Size(max = 500, message = "内容不能超过500字")
    private String content;
}
