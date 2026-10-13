package com.c2c.ai.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * AI 模块配置（app.ai.*）。
 *
 * <p>大模型接入为「可配置」：配置了 {@code api-key} 走真实多模态大模型，
 * 未配置 / 调用失败自动降级本地规则引擎，保证功能任何时候可用（参赛答辩可演示）。</p>
 *
 * <p>真实 Key 放 {@code config/application-pro.yml}（已被 .gitignore 排除），
 * 本文件只保留占位符默认值。</p>
 */
@Data
@Component
@ConfigurationProperties(prefix = "app.ai")
public class AiProperties {

    /** OpenAI 兼容接口 Base URL，如 https://ark.cn-beijing.volces.com/api/v3（留空 = 规则引擎模式） */
    private String baseUrl = "";

    /** API Key（留空 = 规则引擎模式） */
    private String apiKey = "";

    /** 模型名（需支持 vision 多模态） */
    private String model = "doubao-1-5-vision-pro-250828";

    /** 连接 / 读取超时（毫秒） */
    private long timeout = 15000;
}
