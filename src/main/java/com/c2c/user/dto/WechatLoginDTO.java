package com.c2c.user.dto;

import lombok.Data;
import javax.validation.constraints.NotBlank;

/**
 * 微信登录请求：前端 wx.login 拿到的 code。
 */
@Data
public class WechatLoginDTO {
    @NotBlank(message = "登录 code 不能为空")
    private String code;

    /** 微信头像 URL（可选，首次注册时使用） */
    private String avatarUrl;

    /** 微信昵称（可选，首次注册时使用） */
    private String nickname;
}