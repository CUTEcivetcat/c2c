package com.c2c.review.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 管理/审核操作日志实体：违规下架、恢复上架、举报处理、申诉处理、角色分配等留痕。
 * 用于事后审计，确认每次操作的操作人、对象与理由。
 */
@Data
@TableName("admin_log")
public class AdminLog {

    /** 日志 ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 操作人用户 ID（管理员/审核员） */
    private Long operatorId;

    /** 操作人角色：1管理员 2审核员 */
    private Integer operatorRole;

    /** 动作：ban / restore / report_handle / appeal_handle / set_role */
    private String action;

    /** 对象类型：product / report / appeal / user */
    private String targetType;

    /** 对象 ID */
    private Long targetId;

    /** 操作详情（含理由） */
    private String detail;

    /** 操作时间（自动填充） */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
