package com.c2c.product.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.c2c.common.enums.ProductStatus;
import com.c2c.common.exception.BusinessException;
import com.c2c.product.dto.CommentCreateDTO;
import com.c2c.product.dto.CommentVO;
import com.c2c.product.entity.Product;
import com.c2c.product.entity.ProductComment;
import com.c2c.product.feign.UserFeignClient;
import com.c2c.product.mapper.ProductCommentMapper;
import com.c2c.product.mapper.ProductMapper;
import com.c2c.product.service.ProductCommentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.stream.Collectors;

/**
 * 商品评论服务实现类
 * <p>实现商品评论的分页查询、发表/回复与逻辑删除，
 * 通过 Feign 获取评论用户的昵称与头像信息。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProductCommentServiceImpl implements ProductCommentService {

    private static final int MAX_LENGTH = 500;

    private final ProductCommentMapper commentMapper;
    private final ProductMapper productMapper;
    private final UserFeignClient userFeignClient;

    @Override
    public Page<CommentVO> listByProduct(Long productId, int page, int size) {
        Page<ProductComment> result = commentMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<ProductComment>()
                        .eq(ProductComment::getProductId, productId)
                        .eq(ProductComment::getStatus, 1)
                        .orderByAsc(ProductComment::getCreatedAt));

        Page<CommentVO> voPage = new Page<>(page, size, result.getTotal());
        voPage.setRecords(result.getRecords().stream()
                .map(this::buildVO)
                .collect(Collectors.toList()));
        return voPage;
    }

    @Override
    public void add(CommentCreateDTO dto, Long userId) {
        Product product = productMapper.selectById(dto.getProductId());
        if (product == null
                || product.getStatus() == ProductStatus.OFF_SHELF.getCode()
                || product.getStatus() == ProductStatus.BANNED.getCode()) {
            throw new BusinessException("商品不存在或不可评论");
        }

        String content = dto.getContent() == null ? "" : dto.getContent().trim();
        if (StrUtil.isBlank(content)) {
            throw new BusinessException("评论内容不能为空");
        }
        if (content.length() > MAX_LENGTH) {
            throw new BusinessException("评论内容不能超过" + MAX_LENGTH + "字");
        }

        long parentId = dto.getParentId() == null ? 0L : dto.getParentId();
        if (parentId != 0L) {
            ProductComment parent = commentMapper.selectById(parentId);
            if (parent == null || !parent.getProductId().equals(dto.getProductId())) {
                throw new BusinessException("回复的评论不存在");
            }
        }

        ProductComment comment = new ProductComment();
        comment.setProductId(dto.getProductId());
        comment.setUserId(userId);
        comment.setParentId(parentId);
        comment.setContent(content);
        comment.setStatus(1);
        commentMapper.insert(comment);
    }

    @Override
    public void delete(Long commentId, Long userId) {
        ProductComment comment = commentMapper.selectById(commentId);
        if (comment == null || !comment.getUserId().equals(userId)) {
            throw new BusinessException("无权删除该评论");
        }
        comment.setStatus(0);
        commentMapper.updateById(comment);
    }

    private CommentVO buildVO(ProductComment c) {
        String nickname = "";
        String avatar = "";
        try {
            Map<String, Object> userInfo = userFeignClient.getUserPublicInfo(c.getUserId());
            if (userInfo != null && userInfo.get("data") instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> data = (Map<String, Object>) userInfo.get("data");
                nickname = String.valueOf(data.getOrDefault("nickname", ""));
                avatar = String.valueOf(data.getOrDefault("avatarUrl", ""));
            }
        } catch (Exception e) {
            log.warn("获取评论用户信息失败: userId={}", c.getUserId());
        }

        return CommentVO.builder()
                .id(c.getId())
                .productId(c.getProductId())
                .userId(c.getUserId())
                .nickname(nickname)
                .avatar(avatar)
                .parentId(c.getParentId())
                .content(c.getContent())
                .createdAt(c.getCreatedAt())
                .build();
    }
}
