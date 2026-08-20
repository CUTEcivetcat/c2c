package com.c2c.review.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 商品整改申诉实体：商品被违规下架后，卖家提交整改说明申请重新上架。
 * 状态：1 待审核 / 2 已通过（恢复上架）/ 3 已驳回；同商品最多申诉 3 次。
 */
@Data
@TableName("product_appeal")
public class ProductAppeal {

    /** 申诉 ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 商品 ID */
    private Long productId;

    /** 卖家用户 ID */
    private Long sellerId;

    /** 整改说明/申诉理由 */
    private String appealReason;

    /** 整改附图 URL（逗号分隔） */
    private String images;

    /** 状态：1待审核 2已通过(恢复上架) 3已驳回 */
    private Integer status;

    /** 第几次申诉（最多 3 次） */
    private Integer appealCount;

    /** 处理人用户 ID（管理员/审核员） */
    private Long handledBy;

    /** 审核回复（驳回/通过说明） */
    private String reply;

    /** 处理时间 */
    private LocalDateTime handledAt;

    /** 申诉时间（自动填充） */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /** 更新时间（自动填充） */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
