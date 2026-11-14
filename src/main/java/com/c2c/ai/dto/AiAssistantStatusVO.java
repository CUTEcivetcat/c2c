package com.c2c.ai.dto;

import lombok.Builder;
import lombok.Data;

/**
 * 平台助手状态出参：供前端区分「已接入大模型」与「本地规则问答」两种口径。
 */
@Data
@Builder
public class AiAssistantStatusVO {

    /** 助手功能总开关（sys.ai-assistant.enabled） */
    private boolean enabled;

    /** 是否已配置可调用的大模型（Base URL + Key）；false 时走本地规则引擎 */
    private boolean modelConfigured;
}
