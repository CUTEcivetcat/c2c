package com.c2c.ai.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 客服知识库 FAQ：AI 客服的规则问答检索源（关键词命中即答）。
 */
@Data
@TableName("faq")
public class Faq {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 标准问 */
    private String question;

    /** 答案 */
    private String answer;

    /** 分类：交易规则/账号/支付/发货/退款/违规/AI */
    private String category;

    /** 检索关键词（逗号分隔） */
    private String keywords;

    /** 1启用 0停用 */
    private Integer status;

    /** 排序（小的在前） */
    private Integer sortOrder;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
