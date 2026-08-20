package com.c2c.user.dto;

import javax.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 绑定手机号请求参数对象
 */
@Data
public class BindPhoneDTO {

    /** 要绑定的手机号 */
    @NotBlank(message = "phone is required")
    private String phone;

    /** 短信验证码 */
    @NotBlank(message = "verification code is required")
    private String smsCode;
}
