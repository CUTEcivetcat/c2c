package com.c2c.product.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 商品评论实体：商品下的一级 / 二级评论，删除为逻辑删除（status=0）。
 */
@Data
@TableName("product_comment")
public class ProductComment {

    /** 评论 ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 商品 ID */
    private Long productId;

    /** 评论用户 ID */
    private Long userId;

    /** 父评论 ID，0 = 一级评论 */
    private Long parentId;

    /** 评论内容 */
    private String content;

    /** 状态：1 正常 / 0 已删除（逻辑删除） */
    private Integer status;

    /** 评论时间（自动填充） */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
