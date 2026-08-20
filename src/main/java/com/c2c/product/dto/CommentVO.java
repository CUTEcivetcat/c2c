package com.c2c.product.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 商品评论视图对象（对外返回的评论信息）
 */
@Data
@Builder
public class CommentVO {

    /** 评论ID */
    private Long id;

    /** 被评论的商品ID */
    private Long productId;

    /** 评论用户ID */
    private Long userId;

    /** 评论用户昵称 */
    private String nickname;

    /** 评论用户头像 */
    private String avatar;

    /** 父评论ID（回复评论时有效） */
    private Long parentId;

    /** 评论内容 */
    private String content;

    /** 评论创建时间 */
    private LocalDateTime createdAt;
}
