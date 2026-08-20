package com.c2c.product.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import lombok.Data;

/**
 * 商品评论创建请求参数对象
 */
@Data
public class CommentCreateDTO {

    /** 被评论的商品ID */
    @NotNull(message = "商品ID不能为空")
    private Long productId;

    /** 回复时填父评论ID，否则为空 */
    private Long parentId;

    /** 评论内容 */
    @NotBlank(message = "评论内容不能为空")
    private String content;
}
