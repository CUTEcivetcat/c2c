package com.c2c.suggestion.dto;

import lombok.Data;

/**
 * 建议反馈回复请求（管理端）。
 * adopt：true 已采纳 / false 未采纳；reply 为回复意见（可空）。
 */
@Data
public class SuggestionReplyDTO {

    /** 是否采纳 */
    private Boolean adopt;

    /** 回复意见 */
    private String reply;
}
