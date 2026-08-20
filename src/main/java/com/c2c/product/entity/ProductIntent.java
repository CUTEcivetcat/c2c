package com.c2c.product.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 购买意向实体：买家对商品“我想要”询价/砍价，卖家可回复并标记成交。
 */
@Data
@TableName("product_intent")
public class ProductIntent {

    /** 意向 ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 商品 ID */
    private Long productId;

    /** 卖家 ID（冗余，便于卖家查询） */
    private Long sellerId;

    /** 买家 ID */
    private Long buyerId;

    /** 买家留言/询价 */
    private String message;

    /** 买家期望价格 */
    private BigDecimal expectedPrice;

    /** 状态：1 待处理 / 2 已回复 / 3 已成交 / 4 已关闭 */
    private Integer status;

    /** 卖家回复内容 */
    private String sellerReply;

    /** 创建时间（自动填充） */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /** 更新时间（自动填充） */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
