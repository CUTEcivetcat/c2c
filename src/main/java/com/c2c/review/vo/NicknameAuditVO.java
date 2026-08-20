package com.c2c.review.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 昵称审核视图对象：展示申请人、新旧昵称、审核状态与处理结果。
 */
@Data
@Builder
public class NicknameAuditVO {

    /** 记录 ID */
    private Long id;

    /** 申请人用户 ID */
    private Long userId;

    /** 申请人当前昵称 */
    private String userName;

    /** 原昵称 */
    private String oldNickname;

    /** 申请的新昵称 */
    private String newNickname;

    /** 状态：0 待审核 / 1 已通过 / 2 已拒绝 */
    private Integer status;

    /** 处理说明 / 拒绝原因 */
    private String reason;

    /** 申请时间 */
    private LocalDateTime createdAt;

    /** 处理时间 */
    private LocalDateTime handledAt;
}
