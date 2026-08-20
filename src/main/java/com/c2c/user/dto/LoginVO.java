package com.c2c.user.dto;

import lombok.Builder;
import lombok.Data;

/**
 * 登录响应
 */
@Data
@Builder
public class LoginVO {

    /** 访问令牌（Access Token） */
    private String token;

    /** 刷新令牌（用于刷新访问令牌） */
    private String refreshToken;

    /** 访问令牌有效期（秒） */
    private Long expiresIn;

    /** 登录用户信息 */
    private UserVO userInfo;
}
