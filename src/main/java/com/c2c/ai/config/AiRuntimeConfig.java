package com.c2c.ai.config;

import cn.hutool.core.util.StrUtil;
import com.c2c.system.constant.SysConfigKey;
import com.c2c.system.service.SysConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * AI 运行时配置：优先读 sys_config（管理端「AI 接入配置」页热更新），缺省回退 {@link AiProperties}（yml/环境变量）。
 *
 * <p>未配置 Base URL 或 Key 时 {@link #configured()} 返回 false，上层降级本地规则引擎，不发起真实模型调用。</p>
 *
 * @author wzh
 */
@Component
@RequiredArgsConstructor
public class AiRuntimeConfig {

    private final SysConfigService sysConfigService;
    private final AiProperties properties;

    /** OpenAI 兼容 Base URL */
    public String baseUrl() {
        return StrUtil.trim(sysConfigService.get(SysConfigKey.AI_BASE_URL, properties.getBaseUrl()));
    }

    /** API Key（留空 = 规则引擎模式） */
    public String apiKey() {
        return sysConfigService.get(SysConfigKey.AI_API_KEY, properties.getApiKey());
    }

    /** 模型名（客服与多模态发布共用） */
    public String model() {
        String m = StrUtil.trim(sysConfigService.get(SysConfigKey.AI_MODEL, properties.getModel()));
        return StrUtil.isBlank(m) ? properties.getModel() : m;
    }

    /** 调用超时（毫秒） */
    public long timeout() {
        return sysConfigService.getInt(SysConfigKey.AI_TIMEOUT, (int) properties.getTimeout());
    }

    /** 是否已配置可发起真实调用的模型（Base URL + Key 均非空） */
    public boolean configured() {
        return StrUtil.isNotBlank(baseUrl()) && StrUtil.isNotBlank(apiKey());
    }

    /** 配置指纹：任一配置变化即重建模型客户端 */
    public String fingerprint() {
        return baseUrl() + "|" + apiKey() + "|" + model() + "|" + timeout();
    }
}
