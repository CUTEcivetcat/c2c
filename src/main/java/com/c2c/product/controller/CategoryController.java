package com.c2c.product.controller;

import com.c2c.common.constant.ApiPath;
import com.c2c.common.result.R;
import com.c2c.product.entity.Category;
import com.c2c.product.service.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 商品分类（用户端）：提供分类树，供发布/筛选使用。公开接口，无需登录。
 */
@RestController
@RequiredArgsConstructor
@Tag(name = "商品分类", description = "用户端分类树，公开接口")
public class CategoryController {

    private final CategoryService categoryService;

    /**
     * 获取分类树
     */
    @Operation(summary = "获取分类树", description = "返回树形结构，发布与筛选商品时使用")
    @GetMapping(ApiPath.CATEGORY)
    public R<List<Category>> getTree() {
        List<Category> tree = categoryService.getTree();
        return R.ok(tree);
    }
}
