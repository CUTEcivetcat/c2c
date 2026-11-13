package com.c2c.suggestion.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 建议反馈视图对象：建议记录 + 提交人昵称。
 * 供用户端「我的建议」与管理端建议反馈列表展示。
 */
@Data
public class SuggestionVO {

    /** 建议 ID */
    private Long id;

    /** 提交人用户 ID */
    private Long userId;

    /** 提交人昵称 */
    private String nickname;

    /** 建议/反馈内容 */
    private String content;

    /** 联系方式 */
    private String contact;

    /** 状态：1待处理 2已采纳 3未采纳 */
    private Integer status;

    /** 状态中文名 */
    private String statusText;

    /** 管理回复意见 */
    private String reply;

    /** 回复人管理员 ID */
    private Long repliedBy;

    /** 回复时间 */
    private LocalDateTime repliedAt;

    /** 提交时间 */
    private LocalDateTime createdAt;
}
