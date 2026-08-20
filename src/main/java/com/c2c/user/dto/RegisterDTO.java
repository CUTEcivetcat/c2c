package com.c2c.user.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import lombok.Data;

/**
 * 用户注册请求参数对象
 */
@Data
public class RegisterDTO {

    /** 手机号 */
    private String phone;

    /** 邮箱 */
    private String email;

    /** 短信验证码 */
    @NotBlank(message = "verification code is required")
    private String smsCode;

    /** 登录密码（长度 8-32 位） */
    @NotBlank(message = "password is required")
    @Size(min = 8, max = 32, message = "password length must be 8-32")
    private String password;

    /** 昵称 */
    private String nickname;

    /** 个人简介 */
    private String bio;
}
