package com.c2c.user.dto;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 用户信息视图对象（对外返回的用户信息）
 */
@Data
@Builder
public class UserVO {

    /** 用户ID */
    private Long id;

    /** 手机号（脱敏后展示，如 138****8000） */
    private String phone;

    /** 邮箱（脱敏后展示，如 t***@qq.com） */
    private String email;

    /** 昵称 */
    private String nickname;

    /** 待审核昵称（修改昵称提交审核后暂存，通过后生效） */
    private String nicknamePending;

    /** 昵称状态：0 正常 / 1 审核中 */
    private Integer nicknameStatus;

    /** 个人简介 */
    private String bio;

    /** 头像URL */
    private String avatarUrl;

    /** 性别（0=未知，1=男，2=女） */
    private Integer gender;

    /** 角色：0=普通用户，1=管理员，2=审核员 */
    private Integer role;

    /** 注册来源：email / wechat / phone */
    private String loginSource;

    /** 信誉评分 */
    private BigDecimal reputationScore;

    /** 账户余额 */
    private BigDecimal balance;

    /** 最后登录时间 */
    private LocalDateTime lastLoginAt;

    /** 创建时间（注册时间） */
    private LocalDateTime createdAt;
}
