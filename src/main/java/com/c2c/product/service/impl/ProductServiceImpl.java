package com.c2c.product.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.c2c.common.enums.ProductCondition;
import com.c2c.common.enums.ProductStatus;
import com.c2c.common.exception.BusinessException;
import com.c2c.product.dto.ProductCreateDTO;
import com.c2c.product.dto.ProductVO;
import com.c2c.product.entity.Category;
import com.c2c.product.entity.Product;
import com.c2c.product.entity.ProductImage;
import com.c2c.product.feign.UserFeignClient;
import com.c2c.product.mapper.CategoryMapper;
import com.c2c.product.mapper.ProductImageMapper;
import com.c2c.product.mapper.ProductMapper;
import com.c2c.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 商品服务实现类
 * <p>实现商品的发布、编辑、上下架、违规管理与搜索查询，
 * 使用 Redis 缓存商品详情与热度数据，通过 Feign 获取卖家与收藏信息。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductMapper productMapper;
    private final ProductImageMapper imageMapper;
    private final CategoryMapper categoryMapper;
    private final UserFeignClient userFeignClient;
    private final StringRedisTemplate redisTemplate;

    @Override
    @Transactional
    public Long publish(ProductCreateDTO dto, Long sellerId) {
        // 1. 保存商品
        Product product = new Product();
        BeanUtil.copyProperties(dto, product);
        product.setSellerId(sellerId);
        product.setStatus(ProductStatus.ON_SALE.getCode());
        product.setViewCount(0);
        product.setFavoriteCount(0);
        if (product.getFreightType() == null) product.setFreightType(1);
        if (product.getFreightAmount() == null) product.setFreightAmount(BigDecimal.ZERO);
        productMapper.insert(product);

        // 2. 保存图片
        if (dto.getImages() != null && !dto.getImages().isEmpty()) {
            for (int i = 0; i < dto.getImages().size(); i++) {
                ProductImage image = new ProductImage();
                image.setProductId(product.getId());
                image.setUrl(dto.getImages().get(i));
                image.setSortOrder(i);
                image.setIsCover(i == 0 ? 1 : 0);
                imageMapper.insert(image);
            }
        }

        log.info("商品发布成功: id={}, title={}, sellerId={}", product.getId(), product.getTitle(), sellerId);
        return product.getId();
    }

    @Override
    @Transactional
    public void update(Long productId, ProductCreateDTO dto, Long sellerId) {
        Product product = productMapper.selectById(productId);
        if (product == null || !product.getSellerId().equals(sellerId)) {
            throw new BusinessException("无权操作该商品");
        }
        BeanUtil.copyProperties(dto, product, "images");
        productMapper.updateById(product);

        // 更新图片（先删后增）
        imageMapper.delete(new LambdaQueryWrapper<ProductImage>().eq(ProductImage::getProductId, productId));
        if (dto.getImages() != null && !dto.getImages().isEmpty()) {
            for (int i = 0; i < dto.getImages().size(); i++) {
                ProductImage image = new ProductImage();
                image.setProductId(productId);
                image.setUrl(dto.getImages().get(i));
                image.setSortOrder(i);
                image.setIsCover(i == 0 ? 1 : 0);
                imageMapper.insert(image);
            }
        }
        // 清除缓存
        redisTemplate.delete("product:info:" + productId);
    }

    @Override
    public void offShelf(Long productId, Long sellerId) {
        Product product = productMapper.selectById(productId);
        if (product == null || !product.getSellerId().equals(sellerId)) {
            throw new BusinessException("无权操作该商品");
        }
        product.setStatus(ProductStatus.OFF_SHELF.getCode());
        productMapper.updateById(product);
        redisTemplate.delete("product:info:" + productId);
    }

    @Override
    public void updateStatus(Long productId, Long sellerId, Integer status) {
        Product product = productMapper.selectById(productId);
        if (product == null || !product.getSellerId().equals(sellerId)) {
            throw new BusinessException("无权操作该商品");
        }
        if (status == null || status == ProductStatus.BANNED.getCode()) {
            throw new BusinessException("无效的商品状态");
        }
        product.setStatus(status);
        productMapper.updateById(product);
        redisTemplate.delete("product:info:" + productId);
    }

    @Override
    public void ban(Long productId, String reason) {
        Product product = productMapper.selectById(productId);
        if (product == null) {
            throw new BusinessException("商品不存在");
        }
        productMapper.update(null, new LambdaUpdateWrapper<Product>()
                .eq(Product::getId, productId)
                .set(Product::getStatus, ProductStatus.BANNED.getCode())
                .set(Product::getReviewReason, reason == null ? "" : reason));
        redisTemplate.delete("product:info:" + productId);
    }

    @Override
    public void restore(Long productId) {
        Product product = productMapper.selectById(productId);
        if (product == null) {
            throw new BusinessException("商品不存在");
        }
        productMapper.update(null, new LambdaUpdateWrapper<Product>()
                .eq(Product::getId, productId)
                .set(Product::getStatus, ProductStatus.ON_SALE.getCode())
                .set(Product::getReviewReason, null));
        redisTemplate.delete("product:info:" + productId);
    }

    @Override
    public ProductVO getDetail(Long productId, Long currentUserId) {
        String cacheKey = "product:info:" + productId;
        Map<Object, Object> cached = redisTemplate.opsForHash().entries(cacheKey);
        if (!cached.isEmpty()) {
            Product product = BeanUtil.toBeanIgnoreError(cached, Product.class);
            redisTemplate.opsForZSet().incrementScore("product:hot", String.valueOf(productId), 1);
            return buildVO(product, currentUserId);
        }

        Product product = productMapper.selectById(productId);
        if (product == null
                || product.getStatus() == ProductStatus.OFF_SHELF.getCode()
                || product.getStatus() == ProductStatus.BANNED.getCode()) {
            throw new BusinessException(404, "商品不存在或已下架");
        }

        // 浏览量+1
        product.setViewCount(product.getViewCount() + 1);
        productMapper.updateById(product);
        redisTemplate.opsForZSet().incrementScore("product:hot", String.valueOf(productId), 1);

        Map<String, Object> cacheMap = BeanUtil.beanToMap(product, false, true);
        cacheMap.forEach((k, v) -> {
            if (v != null) redisTemplate.opsForHash().put(cacheKey, k, v.toString());
        });
        redisTemplate.expire(cacheKey, 1, TimeUnit.HOURS);

        return buildVO(product, currentUserId);
    }

    @Override
    public Page<ProductVO> search(String keyword, Long categoryId, Integer condition,
                                   Double minPrice, Double maxPrice, String sort,
                                   int page, int size) {
        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Product::getStatus, ProductStatus.ON_SALE.getCode());

        if (StrUtil.isNotBlank(keyword)) {
            wrapper.apply("MATCH(title, description) AGAINST({0})", keyword);
        }
        if (categoryId != null) {
            // 查询所有子分类
            List<Long> catIds = getAllChildCategoryIds(categoryId);
            catIds.add(categoryId);
            wrapper.in(Product::getCategoryId, catIds);
        }
        if (condition != null) {
            wrapper.eq(Product::getCondition, condition);
        }
        if (minPrice != null) {
            wrapper.ge(Product::getPrice, BigDecimal.valueOf(minPrice));
        }
        if (maxPrice != null) {
            wrapper.le(Product::getPrice, BigDecimal.valueOf(maxPrice));
        }

        // 排序
        if ("price_asc".equals(sort)) wrapper.orderByAsc(Product::getPrice);
        else if ("price_desc".equals(sort)) wrapper.orderByDesc(Product::getPrice);
        else if ("view_count".equals(sort)) wrapper.orderByDesc(Product::getViewCount);
        else if ("hot".equals(sort)) wrapper.orderByDesc(Product::getViewCount).orderByDesc(Product::getFavoriteCount);
        else wrapper.orderByDesc(Product::getCreatedAt);

        Page<Product> result = productMapper.selectPage(new Page<>(page, size), wrapper);

        Page<ProductVO> voPage = new Page<>(page, size, result.getTotal());
        voPage.setRecords(result.getRecords().stream()
                .map(p -> buildVO(p, null))
                .collect(Collectors.toList()));
        return voPage;
    }

    @Override
    public Page<ProductVO> getMyPublished(Long sellerId, int page, int size) {
        Page<Product> result = productMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<Product>()
                        .eq(Product::getSellerId, sellerId)
                        .orderByDesc(Product::getCreatedAt));

        Page<ProductVO> voPage = new Page<>(page, size, result.getTotal());
        voPage.setRecords(result.getRecords().stream()
                .map(p -> buildVO(p, null))
                .collect(Collectors.toList()));
        return voPage;
    }

    @Override
    public Page<ProductVO> getUserOnSaleList(Long userId, int page, int size) {
        Page<Product> result = productMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<Product>()
                        .eq(Product::getSellerId, userId)
                        .eq(Product::getStatus, ProductStatus.ON_SALE.getCode())
                        .orderByDesc(Product::getCreatedAt));

        Page<ProductVO> voPage = new Page<>(page, size, result.getTotal());
        voPage.setRecords(result.getRecords().stream()
                .map(p -> buildVO(p, null))
                .collect(Collectors.toList()));
        return voPage;
    }

    // ========== 私有方法 ==========

    private ProductVO buildVO(Product product, Long currentUserId) {
        // 获取图片
        List<ProductImage> images = imageMapper.selectList(
                new LambdaQueryWrapper<ProductImage>()
                        .eq(ProductImage::getProductId, product.getId())
                        .orderByAsc(ProductImage::getSortOrder));

        // 获取卖家信息
        String sellerName = "";
        String sellerAvatar = "";
        BigDecimal sellerReputation = BigDecimal.valueOf(5.0);
        try {
            Map<String, Object> userInfo = userFeignClient.getUserPublicInfo(product.getSellerId());
            if (userInfo != null && userInfo.get("data") != null) {
                @SuppressWarnings("unchecked")
                Map<String, Object> data = (Map<String, Object>) userInfo.get("data");
                sellerName = String.valueOf(data.getOrDefault("nickname", ""));
                sellerAvatar = String.valueOf(data.getOrDefault("avatarUrl", ""));
                Object rep = data.get("reputationScore");
                if (rep != null) sellerReputation = new BigDecimal(rep.toString());
            }
        } catch (Exception e) {
            log.warn("获取卖家信息失败: sellerId={}", product.getSellerId());
        }

        Category category = categoryMapper.selectById(product.getCategoryId());
        String categoryName = category != null ? category.getName() : "";

        // 检查是否已收藏
        boolean isFavorited = false;
        if (currentUserId != null) {
            String favKey = "favorite:check:" + currentUserId + ":" + product.getId();
            String val = redisTemplate.opsForValue().get(favKey);
            isFavorited = "1".equals(val);
        }

        return ProductVO.builder()
                .id(product.getId())
                .sellerId(product.getSellerId())
                .sellerName(sellerName)
                .sellerAvatar(sellerAvatar)
                .sellerReputation(sellerReputation)
                .categoryId(product.getCategoryId())
                .categoryName(categoryName)
                .title(product.getTitle())
                .description(product.getDescription())
                .price(product.getPrice())
                .originalPrice(product.getOriginalPrice())
                .condition(product.getCondition())
                .conditionText(ProductCondition.getTextByCode(product.getCondition()))
                .status(product.getStatus())
                .statusText(ProductStatus.getTextByCode(product.getStatus()))
                .reviewReason(product.getReviewReason())
                .freightType(product.getFreightType())
                .freightText(product.getFreightType() == 1 ? "包邮" : "买家承担运费")
                .freightAmount(product.getFreightAmount())
                .viewCount(product.getViewCount())
                .favoriteCount(product.getFavoriteCount())
                .location(product.getLocation())
                .isFavorited(isFavorited)
                .images(images.stream().map(img -> ProductVO.ImageVO.builder()
                        .id(img.getId())
                        .url(img.getUrl())
                        .isCover(img.getIsCover() == 1)
                        .build()).collect(Collectors.toList()))
                .createdAt(product.getCreatedAt())
                .updatedAt(product.getUpdatedAt())
                .build();
    }

    private List<Long> getAllChildCategoryIds(Long parentId) {
        List<Long> ids = new ArrayList<>();
        List<Category> children = categoryMapper.selectList(
                new LambdaQueryWrapper<Category>().eq(Category::getParentId, parentId));
        for (Category child : children) {
            ids.add(child.getId());
            ids.addAll(getAllChildCategoryIds(child.getId()));
        }
        return ids;
    }
}



