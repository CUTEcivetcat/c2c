package com.c2c.review.dto;

import lombok.Data;

/**
 * 提交举报请求
 */
@Data
public class ReportCreateDTO {

    /** 被举报商品 ID（必填） */
    private Long productId;

    /** 举报类型：1违禁品 2假冒伪劣 3描述不符 4欺诈 5侵权 6其他 */
    private Integer reportType;

    /** 举报理由（必填） */
    private String reason;

    /** 举报附图 URL，逗号分隔 */
    private String images;
}
