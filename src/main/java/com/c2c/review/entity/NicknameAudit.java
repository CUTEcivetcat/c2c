package com.c2c.review.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 昵称修改审核实体：用户申请修改昵称后进入审核，通过后生效、拒绝则保留旧昵称。
 */
@Data
@TableName("nickname_audit")
public class NicknameAudit {

    /** 记录 ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 申请人用户 ID */
    private Long userId;

    /** 原昵称 */
    private String oldNickname;

    /** 申请的新昵称 */
    private String newNickname;

    /** 状态：0 待审核 / 1 已通过 / 2 已拒绝 */
    private Integer status;

    /** 处理说明 / 拒绝原因 */
    private String reason;

    /** 处理人用户 ID（审核员/管理员） */
    private Long handledBy;

    /** 申请时间 */
    private LocalDateTime createdAt;

    /** 处理时间 */
    private LocalDateTime handledAt;
}
