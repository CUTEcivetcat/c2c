package com.c2c.user.dto;

import lombok.Data;
import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;

/**
 * 绑定邮箱请求（微信登录后绑定邮箱，用于找回密码 / 多端登录）。
 */
@Data
public class BindEmailDTO {
    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    private String email;

    @NotBlank(message = "验证码不能为空")
    private String code;
}