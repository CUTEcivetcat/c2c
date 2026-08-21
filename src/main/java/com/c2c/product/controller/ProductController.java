package com.c2c.product.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.c2c.common.constant.ApiPath;
import com.c2c.common.result.R;
import com.c2c.product.dto.ProductCreateDTO;
import com.c2c.product.dto.ProductVO;
import com.c2c.product.entity.Product;
import com.c2c.product.mapper.ProductMapper;
import com.c2c.product.service.ProductService;
import cn.hutool.core.util.StrUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;
import javax.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 商品：发布 / 修改 / 下架 / 详情 / 搜索 / 状态更新 / 我的发布 / 某用户在售商品，
 * 以及管理端的商品列表、统计、违规下架、恢复上架。
 * 公开接口：列表搜索、详情、某用户在售；其余需登录；admin 开头接口需管理员 token。
 */
@RestController
@RequiredArgsConstructor
@Tag(name = "商品", description = "商品发布 / 浏览 / 搜索 / 上下架 / 管理端商品管理")
public class ProductController {

    private final ProductService productService;
    private final ProductMapper productMapper;

    @Operation(summary = "发布商品", description = "需登录")
    @PostMapping(ApiPath.PRODUCT)
    public R<Long> publish(@Valid @RequestBody ProductCreateDTO dto,
                           @Parameter(hidden = true) @RequestHeader("X-User-Id") Long userId) {
        return R.ok(productService.publish(dto, userId));
    }

    @Operation(summary = "修改商品", description = "仅商品发布者本人可改")
    @PutMapping(ApiPath.PRODUCT_ID)
    public R<Void> update(@Parameter(description = "商品 ID") @PathVariable Long id,
                          @Valid @RequestBody ProductCreateDTO dto,
                          @Parameter(hidden = true) @RequestHeader("X-User-Id") Long userId) {
        productService.update(id, dto, userId);
        return R.ok();
    }

    @Operation(summary = "下架商品（删除）", description = "仅发布者本人可下架")
    @DeleteMapping(ApiPath.PRODUCT_ID)
    public R<Void> offShelf(@Parameter(description = "商品 ID") @PathVariable Long id,
                            @Parameter(hidden = true) @RequestHeader("X-User-Id") Long userId) {
        productService.offShelf(id, userId);
        return R.ok();
    }

    @Operation(summary = "商品详情", description = "公开接口，游客可看")
    @GetMapping(ApiPath.PRODUCT_ID)
    public R<ProductVO> detail(@Parameter(description = "商品 ID") @PathVariable Long id,
                               @Parameter(hidden = true) @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        return R.ok(productService.getDetail(id, userId));
    }

    @Operation(summary = "商品搜索 / 列表", description = "公开接口，支持关键字 / 分类 / 成色 / 价格区间筛选")
    @GetMapping(ApiPath.PRODUCT_LIST)
    public R<Page<ProductVO>> search(@Parameter(description = "搜索关键字") @RequestParam(required = false) String keyword,
                                     @Parameter(description = "分类 ID") @RequestParam(required = false) Long categoryId,
                                     @Parameter(description = "成色：如 1 全新 2 九成新 3 八成新 4 七成新及以下") @RequestParam(required = false) Integer condition,
                                     @Parameter(description = "最低价") @RequestParam(required = false) Double minPrice,
                                     @Parameter(description = "最高价") @RequestParam(required = false) Double maxPrice,
                                     @Parameter(description = "排序字段：created_at/price 等") @RequestParam(defaultValue = "created_at") String sort,
                                     @Parameter(description = "页码，从 1 开始") @RequestParam(defaultValue = "1") int page,
                                     @Parameter(description = "每页条数") @RequestParam(defaultValue = "20") int size) {
        return R.ok(productService.search(keyword, categoryId, condition,
                minPrice, maxPrice, sort, page, size));
    }

    @Operation(summary = "更新商品状态（上架/下架/售出）", description = "需登录")
    @PutMapping(ApiPath.PRODUCT_STATUS)
    public R<Void> updateStatus(@Parameter(description = "商品 ID") @PathVariable Long id,
                                @Parameter(hidden = true) @RequestHeader("X-User-Id") Long userId,
                                @Parameter(description = "目标状态，放请求头 X-Status") @RequestHeader("X-Status") Integer status) {
        productService.updateStatus(id, userId, status);
        return R.ok();
    }

    @Operation(summary = "我发布的商品", description = "需登录")
    @GetMapping(ApiPath.PRODUCT_MY_PUBLISHED)
    public R<Page<ProductVO>> myPublished(@Parameter(hidden = true) @RequestHeader("X-User-Id") Long userId,
                                          @Parameter(description = "页码，从 1 开始") @RequestParam(defaultValue = "1") int page,
                                          @Parameter(description = "每页条数") @RequestParam(defaultValue = "20") int size) {
        return R.ok(productService.getMyPublished(userId, page, size));
    }

    @Operation(summary = "查看某用户在售商品", description = "公开接口（用户主页）")
    @GetMapping(ApiPath.PRODUCT_USER_ID)
    public R<Page<ProductVO>> getUserOnSaleList(@Parameter(description = "用户 ID") @PathVariable Long userId,
                                                @Parameter(description = "页码，从 1 开始") @RequestParam(defaultValue = "1") int page,
                                                @Parameter(description = "每页条数") @RequestParam(defaultValue = "20") int size) {
        return R.ok(productService.getUserOnSaleList(userId, page, size));
    }

    @Operation(summary = "批量查询商品（按ID列表）", description = "供最近浏览等场景，ids 逗号分隔，最多 50 个")
    @GetMapping(ApiPath.PRODUCT_IDS)
    public R<List<ProductVO>> batchByIds(@Parameter(description = "商品 ID 列表，逗号分隔") @RequestParam String ids) {
        return R.ok(productService.batchByIds(ids));
    }

    // ==================== Admin 接口 ====================

    @Operation(summary = "商品分页列表（管理端）")
    @GetMapping(ApiPath.PRODUCT_ADMIN_LIST)
    public R<Map<String, Object>> adminList(@Parameter(description = "标题关键字") @RequestParam(required = false) String keyword,
                                            @Parameter(description = "状态筛选") @RequestParam(required = false) Integer status,
                                            @Parameter(description = "页码，从 1 开始") @RequestParam(defaultValue = "1") int page,
                                            @Parameter(description = "每页条数") @RequestParam(defaultValue = "20") int size) {
        LambdaQueryWrapper<Product> w = new LambdaQueryWrapper<>();
        if (status != null) w.eq(Product::getStatus, status);
        if (StrUtil.isNotBlank(keyword)) w.like(Product::getTitle, keyword);
        w.orderByDesc(Product::getCreatedAt);
        Page<Product> result = productMapper.selectPage(new Page<>(page, size), w);
        return R.ok(com.c2c.common.utils.MapUtils.of("records", result.getRecords(), "total", result.getTotal(), "page", page, "size", size));
    }

    @Operation(summary = "商品总数统计（管理端）")
    @GetMapping(ApiPath.PRODUCT_ADMIN_COUNT)
    public R<Map<String, Long>> countProducts() {
        return R.ok(com.c2c.common.utils.MapUtils.of(
            "total", productMapper.selectCount(null),
            "onSale", productMapper.selectCount(new LambdaQueryWrapper<Product>().eq(Product::getStatus, 1))
        ));
    }

    /** 管理员违规下架商品（带原因） */
    @Operation(summary = "违规下架商品（管理端）")
    @PutMapping(ApiPath.PRODUCT_ADMIN_BAN)
    public R<Void> ban(@Parameter(description = "商品 ID") @PathVariable Long id,
                       @RequestBody Map<String, String> body) {
        productService.ban(id, body.getOrDefault("reason", ""));
        return R.ok();
    }

    /** 管理员恢复上架 */
    @Operation(summary = "恢复上架（管理端）")
    @PutMapping(ApiPath.PRODUCT_ADMIN_RESTORE)
    public R<Void> restore(@Parameter(description = "商品 ID") @PathVariable Long id) {
        productService.restore(id);
        return R.ok();
    }
}
