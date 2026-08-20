package com.c2c.review.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 商品举报实体：用户对违规商品发起举报，由审核员/管理员处理。
 * 状态：1 待处理 / 2 已违规下架 / 3 已驳回；处理结果落库并记入 admin_log。
 */
@Data
@TableName("report")
public class Report {

    /** 举报 ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 举报人用户 ID */
    private Long reporterId;

    /** 被举报商品 ID */
    private Long productId;

    /** 举报类型：1违禁品 2假冒伪劣 3描述不符 4欺诈 5侵权 6其他 */
    private Integer reportType;

    /** 举报理由 */
    private String reason;

    /** 举报附图 URL（逗号分隔） */
    private String images;

    /** 状态：1待处理 2已违规下架 3已驳回 */
    private Integer status;

    /** 处理人用户 ID（管理员/审核员） */
    private Long handledBy;

    /** 处理备注/驳回理由 */
    private String handleRemark;

    /** 处理时间 */
    private LocalDateTime handledAt;

    /** 举报时间（自动填充） */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /** 更新时间（自动填充） */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
