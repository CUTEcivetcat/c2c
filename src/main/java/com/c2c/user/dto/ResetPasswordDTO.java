package com.c2c.user.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import lombok.Data;

/**
 * 重置密码请求参数对象
 */
@Data
public class ResetPasswordDTO {

    /** 账号（手机号或邮箱） */
    @NotBlank(message = "account is required")
    private String account;

    /** 短信验证码 */
    @NotBlank(message = "verification code is required")
    private String smsCode;

    /** 新密码（长度 8-32 位） */
    @NotBlank(message = "new password is required")
    @Size(min = 8, max = 32, message = "password length must be 8-32")
    private String newPassword;
}
