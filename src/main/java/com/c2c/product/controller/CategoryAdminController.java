package com.c2c.product.controller;

import com.c2c.common.constant.ApiPath;
import com.c2c.common.result.R;
import com.c2c.product.dto.CategoryCreateDTO;
import com.c2c.product.dto.CategoryUpdateDTO;
import com.c2c.product.entity.Category;
import com.c2c.product.service.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

/**
 * 分类管理接口（管理端）：分类的增删改查。
 * 仅管理员可访问，由 AuthTokenFilter 对 /product/admin/** 统一鉴权（需带 role=ADMIN 的 token）。
 */
@RestController
@RequiredArgsConstructor
@Tag(name = "商品分类（管理端）", description = "分类增删改查，仅管理员可访问")
public class CategoryAdminController {

    private final CategoryService categoryService;

    /** 扁平列表（管理端用，含全部层级，按 sortOrder 排序） */
    @Operation(summary = "分类列表（含全部层级）")
    @GetMapping(ApiPath.CATEGORY_ADMIN)
    public R<List<Category>> listAll() {
        return R.ok(categoryService.listAll());
    }

    @Operation(summary = "新增分类")
    @PostMapping(ApiPath.CATEGORY_ADMIN)
    public R<Category> create(@Valid @RequestBody CategoryCreateDTO dto) {
        return R.ok(categoryService.create(dto));
    }

    @Operation(summary = "修改分类")
    @PutMapping(ApiPath.CATEGORY_ADMIN_ID)
    public R<Category> update(@Parameter(description = "分类 ID") @PathVariable Long id,
                              @Valid @RequestBody CategoryUpdateDTO dto) {
        return R.ok(categoryService.update(id, dto));
    }

    @Operation(summary = "删除分类")
    @DeleteMapping(ApiPath.CATEGORY_ADMIN_ID)
    public R<Void> delete(@Parameter(description = "分类 ID") @PathVariable Long id) {
        categoryService.delete(id);
        return R.ok();
    }
}
