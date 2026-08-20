package com.c2c.user.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 用户实体：账号、资料与信誉分。
 * role=1 表示管理员（见 AdminAuthController），status=0 表示已禁用。
 */
@Data
@TableName("user")
public class User {

    /** 用户 ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户名 */
    private String username;

    /** 密码（BCrypt 加密存储，JSON 序列化时忽略） */
    @JsonIgnore
    private String password;

    /** 手机号 */
    private String phone;

    /** 邮箱 */
    private String email;

    /** 邮箱是否已验证：0 未验证 / 1 已验证 */
    private Integer emailVerified;

    /** 头像 URL */
    private String avatarUrl;

    /** 昵称 */
    private String nickname;

    /** 个人简介 */
    private String bio;

    /** 性别：0 未知 / 1 男 / 2 女 */
    private Integer gender;

    /** 账号状态：1 正常 / 0 禁用 */
    private Integer status;

    /** 角色：0=普通用户，1=管理员, 2=审核 */
    private Integer role;

    /** 信誉分（0~5） */
    private BigDecimal reputationScore;

    /** 最近登录时间 */
    private LocalDateTime lastLoginAt;

    /** 创建时间（自动填充） */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /** 更新时间（自动填充） */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
