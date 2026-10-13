package com.c2c.ai.dto;

import lombok.Data;

/**
 * AI 智能发布请求：首图 URL + 用户已选分类（可选）。
 */
@Data
public class AiPublishRequest {

    /** 首图 URL（必填，用于多模态识别） */
    private String imageUrl;

    /** 用户已选分类 ID（可选，规则引擎兜底时回填分类名） */
    private Long categoryId;
}
