package com.c2c.review.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 举报信息视图对象：举报记录 + 被举报商品摘要 + 举报人/卖家昵称。
 * 供审核工作台与「我的举报」列表展示。
 */
@Data
public class ReportVO {

    /** 举报 ID */
    private Long id;

    /** 举报人用户 ID */
    private Long reporterId;

    /** 举报人昵称 */
    private String reporterNickname;

    /** 被举报商品 ID */
    private Long productId;

    /** 商品标题 */
    private String productTitle;

    /** 商品封面图 */
    private String productCover;

    /** 商品价格 */
    private BigDecimal productPrice;

    /** 商品当前状态：1在售 2已预订 3已售 4下架 5违规下架 */
    private Integer productStatus;

    /** 商品违规原因（当前） */
    private String productReviewReason;

    /** 卖家用户 ID */
    private Long sellerId;

    /** 卖家昵称 */
    private String sellerNickname;

    /** 举报类型编码 */
    private Integer reportType;

    /** 举报类型中文名 */
    private String reportTypeText;

    /** 举报理由 */
    private String reason;

    /** 举报附图 URL */
    private String images;

    /** 状态：1待处理 2已违规下架 3已驳回 */
    private Integer status;

    /** 状态中文名 */
    private String statusText;

    /** 处理备注/驳回理由 */
    private String handleRemark;

    /** 处理人用户 ID */
    private Long handledBy;

    /** 处理时间 */
    private LocalDateTime handledAt;

    /** 举报时间 */
    private LocalDateTime createdAt;
}
