package com.c2c.ai.service;

import com.c2c.ai.dto.AiPublishRequest;
import com.c2c.ai.dto.AiPublishVO;

/**
 * AI 智能发布：多模态识别 → 自动生成标题/描述/分类 + 历史成交价修正估价区间。
 */
public interface AiService {

    /**
     * 智能填写：优先大模型，未配置/失败降级规则引擎；两路径共用历史成交均价修正。
     */
    AiPublishVO intelligentPublish(AiPublishRequest request);
}
