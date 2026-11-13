package com.c2c.suggestion.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户意见建议反馈实体：用户提交建议，管理端查看并回复是否采纳。
 * 状态：1 待处理 / 2 已采纳 / 3 未采纳。
 */
@Data
@TableName("suggestion")
public class Suggestion {

    /** 建议 ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 提交人用户 ID */
    private Long userId;

    /** 建议/反馈内容 */
    private String content;

    /** 联系方式（可空） */
    private String contact;

    /** 状态：1待处理 2已采纳 3未采纳 */
    private Integer status;

    /** 管理回复意见 */
    private String reply;

    /** 回复人管理员 ID */
    private Long repliedBy;

    /** 回复时间 */
    private LocalDateTime repliedAt;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
