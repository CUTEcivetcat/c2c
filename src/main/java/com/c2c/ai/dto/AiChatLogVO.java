package com.c2c.ai.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 管理端 AI 客服对话日志：一条助手回复与其对应的用户提问。
 *
 * <p>来源 source：model=大模型 / rule=规则引擎兜底；question 为同会话内回复之前最近一条用户消息。</p>
 */
@Data
public class AiChatLogVO {

    /** 助手回复消息 ID */
    private Long messageId;

    /** 会话 ID */
    private Long conversationId;

    /** 提问用户 ID */
    private Long userId;

    /** 提问用户昵称 */
    private String userNickname;

    /** 用户提问 */
    private String question;

    /** 助手回复 */
    private String answer;

    /** 来源：model / rule */
    private String source;

    /** 回复时间 */
    private LocalDateTime createdAt;
}
