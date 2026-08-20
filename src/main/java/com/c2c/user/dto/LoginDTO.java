package com.c2c.user.dto;

import javax.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 用户登录请求参数对象
 */
@Data
public class LoginDTO {

    /** 登录账号（手机号或邮箱） */
    @NotBlank(message = "account is required")
    private String account;

    /** 登录密码（密码登录时填写） */
    private String password;

    /** 短信验证码（短信验证码登录时填写） */
    private String smsCode;

    /** 登录类型（如 1=密码登录，2=短信验证码登录） */
    private Integer loginType;
}
