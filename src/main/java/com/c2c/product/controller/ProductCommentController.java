package com.c2c.product.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.c2c.common.constant.ApiPath;
import com.c2c.common.result.R;
import com.c2c.product.dto.CommentCreateDTO;
import com.c2c.product.dto.CommentVO;
import com.c2c.product.service.ProductCommentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

/**
 * 商品评论：查看（公开）/ 发表（需登录）/ 删除（本人或管理员）。
 * 发表与删除需登录；查看游客可看。
 */
@RestController
@RequiredArgsConstructor
@Tag(name = "商品评论", description = "查看（公开）/ 发表（需登录）/ 删除（本人或管理员）")
public class ProductCommentController {

    private final ProductCommentService commentService;

    @Operation(summary = "某商品评论（分页）", description = "公开接口")
    @GetMapping(ApiPath.COMMENT)
    public R<Page<CommentVO>> list(@Parameter(description = "商品 ID") @RequestParam Long productId,
                                   @Parameter(description = "页码，从 1 开始") @RequestParam(defaultValue = "1") int page,
                                   @Parameter(description = "每页条数") @RequestParam(defaultValue = "20") int size) {
        return R.ok(commentService.listByProduct(productId, page, size));
    }

    @Operation(summary = "发表评论", description = "需登录")
    @PostMapping(ApiPath.COMMENT)
    public R<Void> add(@Valid @RequestBody CommentCreateDTO dto,
                       @Parameter(hidden = true) @RequestHeader("X-User-Id") Long userId) {
        commentService.add(dto, userId);
        return R.ok();
    }

    @Operation(summary = "删除评论", description = "仅评论作者本人或管理员可删")
    @DeleteMapping(ApiPath.COMMENT_ID)
    public R<Void> delete(@Parameter(description = "评论 ID") @PathVariable Long id,
                          @Parameter(hidden = true) @RequestHeader("X-User-Id") Long userId) {
        commentService.delete(id, userId);
        return R.ok();
    }
}
