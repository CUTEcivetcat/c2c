package com.c2c.product.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.c2c.product.dto.CommentCreateDTO;
import com.c2c.product.dto.CommentVO;

/**
 * 商品评论服务接口
 * <p>提供商品评论的分页查询、发表（支持对评论的回复）与删除能力。</p>
 */
public interface ProductCommentService {

    /** 分页查询某商品的评论列表（仅展示正常状态的评论） */
    Page<CommentVO> listByProduct(Long productId, int page, int size);

    /** 发表评论或回复（校验商品状态、内容非空与长度） */
    void add(CommentCreateDTO dto, Long userId);

    /** 删除自己的评论（逻辑删除，置为不可见） */
    void delete(Long commentId, Long userId);
}
