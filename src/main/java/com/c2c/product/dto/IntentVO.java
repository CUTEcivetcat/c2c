package com.c2c.product.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 购买意向（询价）视图对象（对外返回的询价信息）
 */
@Data
@Builder
public class IntentVO {

    /** 意向ID */
    private Long id;

    /** 关联的商品ID */
    private Long productId;

    /** 商品标题 */
    private String productTitle;

    /** 商品封面图 */
    private String productCover;

    /** 商品价格 */
    private BigDecimal productPrice;

    /** 卖家用户ID */
    private Long sellerId;

    /** 买家用户ID */
    private Long buyerId;

    /** 买家昵称 */
    private String buyerNickname;

    /** 买家头像 */
    private String buyerAvatar;

    /** 买家留言/询价内容 */
    private String message;

    /** 买家期望价格 */
    private BigDecimal expectedPrice;

    /** 意向状态：1 待处理 / 2 已回复 / 3 已成交 / 4 已关闭 */
    private Integer status;

    /** 状态文本描述 */
    private String statusText;

    /** 卖家回复内容 */
    private String sellerReply;

    /** 创建时间 */
    private LocalDateTime createdAt;
}
