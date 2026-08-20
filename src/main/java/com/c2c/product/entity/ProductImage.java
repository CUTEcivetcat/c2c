package com.c2c.product.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 商品图片实体：一个商品的多张图片，可标记封面。
 */
@Data
@TableName("product_image")
public class ProductImage {

    /** 图片 ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属商品 ID */
    private Long productId;

    /** 图片访问 URL */
    private String url;

    /** 排序权重（越小越靠前） */
    private Integer sortOrder;

    /** 是否封面图：1 是 / 0 否 */
    private Integer isCover;

    /** 创建时间（自动填充） */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
