package com.c2c.ai.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * AI 智能发布出参：一键回填标题 / 描述 / 分类 / 成色 / 建议价区间。
 *
 * <p>{@code source} 标识结果来源：{@code model} = 大模型（可能带历史成交价修正），
 * {@code rule} = 规则引擎兜底。</p>
 */
@Data
public class AiPublishVO {

    /** 建议标题（规则引擎模式下为空串，由用户填写） */
    private String title;

    /** 建议描述（规则引擎模式下为空串，由用户填写） */
    private String description;

    /** 匹配到的分类 ID（匹配不到为 null，前端提示用户手选） */
    private Long categoryId;

    /** 匹配到的分类名（二级分类名） */
    private String categoryName;

    /** 成色：1 全新 / 2 几乎全新 / 3 轻微使用 / 4 明显使用 */
    private Integer condition;

    /** 建议最低价（可空） */
    private BigDecimal priceMin;

    /** 建议最高价（可空） */
    private BigDecimal priceMax;

    /** 结果来源：model / rule */
    private String source;
}
