package com.c2c.suggestion.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 提交意见建议请求
 */
@Data
public class SuggestionCreateDTO {

    /** 建议/反馈内容（必填） */
    @NotBlank(message = "请填写建议内容")
    @Size(max = 1000, message = "建议内容不能超过 1000 字")
    private String content;

    /** 联系方式（可空，手机号/邮箱/微信） */
    @Size(max = 50, message = "联系方式不能超过 50 字")
    private String contact;
}
