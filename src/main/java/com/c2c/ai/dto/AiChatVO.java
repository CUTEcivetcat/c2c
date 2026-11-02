package com.c2c.ai.dto;

import lombok.Builder;
import lombok.Data;

/**
 * AI 客服单轮问答出参。
 */
@Data
@Builder
public class AiChatVO {

    /** 助手回复消息 ID（message 表） */
    private Long messageId;

    /** 回复内容 */
    private String content;

    /** 来源：model=大模型 / rule=规则引擎（FAQ/工具直查兜底） */
    private String source;
}
