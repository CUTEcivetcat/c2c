package com.c2c.product.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 商品实体：二手商品的标题、价格、成色、状态与统计数据。
 */
@Data
@TableName("product")
public class Product {

    /** 商品 ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 卖家用户 ID */
    private Long sellerId;

    /** 分类 ID */
    private Long categoryId;

    /** 标题 */
    private String title;

    /** 描述 */
    private String description;

    /** 售价 */
    private BigDecimal price;

    /** 原价（可空，用于展示折扣） */
    private BigDecimal originalPrice;

    /** 成色：1 全新 / 2 几乎全新 / 3 轻微使用 / 4 明显使用 */
    @TableField("`condition`")
    private Integer condition;

    /** 状态：1 在售 / 2 已预订 / 3 已售 / 4 下架 / 5 违规下架 */
    @TableField("`status`")
    private Integer status;

    /** 违规下架原因（管理员填写） */
    private String reviewReason;

    /** 运费类型：1 包邮 / 2 买家自付 */
    private Integer freightType;

    /** 运费金额（包邮时为 0） */
    private BigDecimal freightAmount;

    /** 浏览量 */
    private Integer viewCount;

    /** 收藏数 */
    private Integer favoriteCount;

    /** 所在地区（展示用） */
    private String location;

    /** 发布时间（自动填充） */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /** 更新时间（自动填充） */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
