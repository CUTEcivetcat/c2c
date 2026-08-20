package com.c2c.admin.feign;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.c2c.product.entity.Product;
import com.c2c.product.mapper.ProductMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 调用商品模块的 Feign 客户端：供管理后台查询商品列表、修改商品状态、统计商品数量（单体模式下基于 Mapper 直接实现）。
 */
@Component("adminProductFeignClient")
@RequiredArgsConstructor
public class ProductFeignClient {
    private final ProductMapper productMapper;

    /** 分页查询商品列表，可按关键字、状态过滤，按创建时间倒序。 */
    public Map<String, Object> getProductList(String keyword, Integer status, int page, int size) {
        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<>();
        if (status != null) {
            wrapper.eq(Product::getStatus, status);
        }
        if (StrUtil.isNotBlank(keyword)) {
            wrapper.like(Product::getTitle, keyword);
        }
        wrapper.orderByDesc(Product::getCreatedAt);
        Page<Product> result = productMapper.selectPage(new Page<Product>(page, size), wrapper);

        Map<String, Object> data = new HashMap<>();
        data.put("records", result.getRecords());
        data.put("total", result.getTotal());
        data.put("page", page);
        data.put("size", size);
        return ok(data);
    }

    /** 更新商品状态（上架/下架等）。 */
    public Map<String, Object> updateStatus(Long id, Integer status) {
        Product product = new Product();
        product.setId(id);
        product.setStatus(status);
        productMapper.updateById(product);
        return ok(null);
    }

    /** 统计商品总数与在售数量。 */
    public Map<String, Object> countProducts() {
        Map<String, Object> data = new HashMap<>();
        data.put("total", productMapper.selectCount(null));
        data.put("onSale", productMapper.selectCount(new LambdaQueryWrapper<Product>().eq(Product::getStatus, 1)));
        return ok(data);
    }

    /** 构造统一成功响应。 */
    private Map<String, Object> ok(Object data) {
        Map<String, Object> map = new HashMap<>();
        map.put("code", 200);
        map.put("message", "success");
        map.put("data", data);
        return map;
    }
}

