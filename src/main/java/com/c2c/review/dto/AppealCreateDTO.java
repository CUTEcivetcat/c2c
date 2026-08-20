package com.c2c.review.dto;

import lombok.Data;

/**
 * 提交整改申诉请求（违规下架商品重新上架）
 */
@Data
public class AppealCreateDTO {

    /** 商品 ID（必填，须为当前用户自己违规下架的商品） */
    private Long productId;

    /** 整改说明/申诉理由（必填） */
    private String appealReason;

    /** 整改附图 URL，逗号分隔 */
    private String images;
}
