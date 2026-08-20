package com.c2c.order.feign;

import com.c2c.product.dto.ProductVO;
import com.c2c.product.entity.Product;
import com.c2c.product.mapper.ProductMapper;
import com.c2c.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 调用商品模块的 Feign 客户端：供订单模块获取商品详情、更新商品状态（单体模式下基于 Service/Mapper 直接实现）。
 */
@Component("orderProductFeignClient")
@RequiredArgsConstructor
public class ProductFeignClient {
    private final ProductService productService;
    private final ProductMapper productMapper;

    /** 获取商品信息（含买家视角的详情）。 */
    public Map<String, Object> getProduct(Long productId, Long userId) {
        ProductVO product = productService.getDetail(productId, userId);
        Map<String, Object> data = new HashMap<>();
        data.put("id", product.getId());
        data.put("sellerId", product.getSellerId());
        data.put("status", product.getStatus());
        data.put("title", product.getTitle());
        data.put("price", product.getPrice());
        data.put("freightAmount", product.getFreightAmount());
        data.put("images", product.getImages());
        return ok(data);
    }

    /** 更新商品状态（如下单后锁定、售出下架）。 */
    public Map<String, Object> updateStatus(Long productId, Integer status) {
        Product product = new Product();
        product.setId(productId);
        product.setStatus(status);
        productMapper.updateById(product);
        return ok(null);
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

