package com.c2c.review.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 整改申诉视图对象：申诉记录 + 商品摘要 + 卖家昵称。
 * 供审核工作台与「我的整改申诉」列表展示。
 */
@Data
public class AppealVO {

    /** 申诉 ID */
    private Long id;

    /** 商品 ID */
    private Long productId;

    /** 商品标题 */
    private String productTitle;

    /** 商品封面图 */
    private String productCover;

    /** 商品价格 */
    private BigDecimal productPrice;

    /** 商品当前状态 */
    private Integer productStatus;

    /** 商品违规原因（下架原因） */
    private String productReviewReason;

    /** 卖家用户 ID */
    private Long sellerId;

    /** 卖家昵称 */
    private String sellerNickname;

    /** 整改说明/申诉理由 */
    private String appealReason;

    /** 整改附图 URL */
    private String images;

    /** 状态：1待审核 2已通过(恢复上架) 3已驳回 */
    private Integer status;

    /** 状态中文名 */
    private String statusText;

    /** 第几次申诉 */
    private Integer appealCount;

    /** 审核回复 */
    private String reply;

    /** 处理人用户 ID */
    private Long handledBy;

    /** 处理时间 */
    private LocalDateTime handledAt;

    /** 申诉时间 */
    private LocalDateTime createdAt;
}
