package com.c2c.product.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.c2c.common.enums.ProductCondition;
import com.c2c.common.enums.ProductStatus;
import com.c2c.common.exception.BusinessException;
import com.c2c.common.utils.PageUtil;
import com.c2c.product.dto.ProductCreateDTO;
import com.c2c.product.dto.ProductVO;
import com.c2c.product.entity.Category;
import com.c2c.product.entity.Product;
import com.c2c.product.entity.ProductImage;
import com.c2c.product.delegate.UserDelegate;
import com.c2c.product.mapper.CategoryMapper;
import com.c2c.product.mapper.ProductImageMapper;
import com.c2c.product.mapper.ProductMapper;
import com.c2c.product.service.FileStorageService;
import com.c2c.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
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
    private final UserDelegate userDelegate;
    private final FileStorageService fileStorageService;
    private final StringRedisTemplate redisTemplate;
    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper;

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
                image.setThumbUrl(fileStorageService.ensureThumbForUrl(dto.getImages().get(i)));
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

        // 更新图片（先增后删：先落新图记录，再清理被替换的旧记录，避免中间态丢图；物理文件不在此主动清理）
        List<ProductImage> oldImages = imageMapper.selectList(
                new LambdaQueryWrapper<ProductImage>().eq(ProductImage::getProductId, productId));
        List<String> newUrls = dto.getImages() == null ? Collections.emptyList() : dto.getImages();
        Map<String, ProductImage> oldByUrl = oldImages.stream()
                .collect(Collectors.toMap(ProductImage::getUrl, img -> img, (a, b) -> a));

        for (int i = 0; i < newUrls.size(); i++) {
            String url = newUrls.get(i);
            ProductImage exist = oldByUrl.get(url);
            if (exist == null) {
                ProductImage image = new ProductImage();
                image.setProductId(productId);
                image.setUrl(url);
                image.setThumbUrl(fileStorageService.ensureThumbForUrl(url));
                image.setSortOrder(i);
                image.setIsCover(i == 0 ? 1 : 0);
                imageMapper.insert(image);
            } else {
                // 保留的旧图按新顺序/封面位更新
                exist.setSortOrder(i);
                exist.setIsCover(i == 0 ? 1 : 0);
                imageMapper.updateById(exist);
            }
        }
        // 删除不再保留的旧图记录
        for (ProductImage old : oldImages) {
            if (!newUrls.contains(old.getUrl())) {
                imageMapper.deleteById(old.getId());
            }
        }
        // 清除缓存
        redisTemplate.delete("product:info:" + productId);
        clearHomeCache();
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
        clearHomeCache();
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
        clearHomeCache();
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
        clearHomeCache();
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
        clearHomeCache();
    }

    @Override
    public ProductVO getDetail(Long productId, Long currentUserId) {
        String cacheKey = "product:info:" + productId;
        Product product = readDetailCache(cacheKey);

        if (product == null) {
            product = productMapper.selectById(productId);
            if (product == null
                    || product.getStatus() == ProductStatus.OFF_SHELF.getCode()
                    || product.getStatus() == ProductStatus.BANNED.getCode()) {
                throw new BusinessException(404, "商品不存在或已下架");
            }
            writeDetailCache(cacheKey, product);
        }

        // 浏览量 SQL 层原子自增（view_count = view_count + 1），并发不丢更新；内存计数 +1 用于本次展示
        productMapper.update(null, new LambdaUpdateWrapper<Product>()
                .eq(Product::getId, productId)
                .setSql("view_count = view_count + 1"));
        product.setViewCount(product.getViewCount() == null ? 1 : product.getViewCount() + 1);
        redisTemplate.opsForZSet().incrementScore("product:hot", String.valueOf(productId), 1);

        ProductVO vo = buildVO(product, currentUserId);
        log.info("商品详情: id={}, title={}, price={}, sellerId={}", product.getId(), product.getTitle(), product.getPrice(), product.getSellerId());
        return vo;
    }

    /** 读取详情缓存（JSON 整对象，避免 Hash 序列化导致 LocalDateTime/BigDecimal 类型失真） */
    private Product readDetailCache(String cacheKey) {
        String cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached == null) {
            return null;
        }
        try {
            return objectMapper.readValue(cached, Product.class);
        } catch (Exception e) {
            // 缓存损坏/旧格式 → 回源数据库重建
            return null;
        }
    }

    /** 写入详情缓存（JSON），1 小时过期；缓存失败不影响查询 */
    private void writeDetailCache(String cacheKey, Product product) {
        try {
            redisTemplate.opsForValue().set(cacheKey, objectMapper.writeValueAsString(product), 1, TimeUnit.HOURS);
        } catch (Exception ignored) {
            // 缓存序列化失败不阻断主流程
        }
    }

    @Override
    public Page<ProductVO> search(String keyword, Long categoryId, Integer condition,
                                   Double minPrice, Double maxPrice, String sort,
                                   int page, int size) {
        page = PageUtil.normalizePage(page);
        size = PageUtil.normalizeSize(size);
        // 首页默认列表（无筛选、第一页、按最新）缓存 60 秒，降低 DB 压力
        boolean homeList = StrUtil.isBlank(keyword) && categoryId == null && condition == null
                && minPrice == null && maxPrice == null && page == 1
                && "created_at".equals(sort);
        String cacheKey = null;
        if (homeList) {
            cacheKey = "product:home:" + size;
            String cached = redisTemplate.opsForValue().get(cacheKey);
            if (cached != null) {
                try {
                    // 缓存内容为 {total, records}；total 必须一并缓存，否则命中缓存时分页总数会变 0
                    com.fasterxml.jackson.databind.JsonNode node = objectMapper.readTree(cached);
                    if (node != null && node.isObject() && node.has("records")) {
                        Page<ProductVO> p = new Page<>(1, size, node.path("total").asLong(0));
                        p.setRecords(objectMapper.convertValue(node.get("records"),
                                new com.fasterxml.jackson.core.type.TypeReference<List<ProductVO>>() {}));
                        return p;
                    }
                } catch (Exception ignored) {
                    // 缓存解析失败、或为旧格式（纯 records 数组）→ 回退到数据库查询
                }
            }
        }

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
        voPage.setRecords(buildVOList(result.getRecords(), null));

        // 写入首页缓存
        if (cacheKey != null) {
            try {
                com.fasterxml.jackson.databind.node.ObjectNode payload = objectMapper.createObjectNode();
                payload.put("total", voPage.getTotal());
                payload.set("records", objectMapper.valueToTree(voPage.getRecords()));
                redisTemplate.opsForValue().set(cacheKey, payload.toString(), 60, TimeUnit.SECONDS);
            } catch (Exception ignored) {
                // 缓存失败不影响查询结果
            }
        }
        log.info("商品列表: keyword={}, categoryId={}, condition={}, page={}, size={}, total={}, rows={}",
                keyword, categoryId, condition, page, size, voPage.getTotal(), voPage.getRecords().size());
        return voPage;
    }

    @Override
    public Page<ProductVO> getMyPublished(Long sellerId, int page, int size) {
        page = PageUtil.normalizePage(page);
        size = PageUtil.normalizeSize(size);
        Page<Product> result = productMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<Product>()
                        .eq(Product::getSellerId, sellerId)
                        .orderByDesc(Product::getCreatedAt));

        Page<ProductVO> voPage = new Page<>(page, size, result.getTotal());
        voPage.setRecords(buildVOList(result.getRecords(), null));
        return voPage;
    }

    @Override
    public Page<ProductVO> getUserOnSaleList(Long userId, int page, int size) {
        page = PageUtil.normalizePage(page);
        size = PageUtil.normalizeSize(size);
        Page<Product> result = productMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<Product>()
                        .eq(Product::getSellerId, userId)
                        .eq(Product::getStatus, ProductStatus.ON_SALE.getCode())
                        .orderByDesc(Product::getCreatedAt));

        Page<ProductVO> voPage = new Page<>(page, size, result.getTotal());
        voPage.setRecords(buildVOList(result.getRecords(), null));
        return voPage;
    }

    @Override
    public java.util.List<ProductVO> batchByIds(String ids) {
        if (ids == null || ids.trim().isEmpty()) {
            return java.util.Collections.emptyList();
        }
        java.util.List<Long> idList = new java.util.ArrayList<>();
        for (String s : ids.split(",")) {
            try {
                idList.add(Long.valueOf(s.trim()));
            } catch (NumberFormatException ignored) {
                // 忽略非法 ID
            }
        }
        if (idList.size() > 50) {
            idList = idList.subList(0, 50);
        }
        if (idList.isEmpty()) {
            return java.util.Collections.emptyList();
        }
        java.util.Map<Long, Product> map = productMapper.selectBatchIds(idList).stream()
                .collect(java.util.stream.Collectors.toMap(Product::getId, p -> p));
        List<Product> ordered = idList.stream()
                .filter(map::containsKey)
                .map(map::get)
                .collect(Collectors.toList());
        return buildVOList(ordered, null);
    }

    // ========== 私有方法 ==========

    private void clearHomeCache() {
        try {
            Set<String> keys = scanKeys("product:home:*");
            if (!keys.isEmpty()) {
                redisTemplate.delete(keys);
            }
        } catch (Exception ignored) {
            // 缓存清理失败不影响业务
        }
    }

    /** SCAN 游标分批扫描匹配 key，避免 KEYS 命令在大库上阻塞 Redis */
    private Set<String> scanKeys(String pattern) {
        Set<String> keys = new HashSet<>();
        redisTemplate.execute((RedisCallback<Void>) connection -> {
            try (Cursor<byte[]> cursor = connection.scan(
                    ScanOptions.scanOptions().match(pattern).count(200).build())) {
                while (cursor.hasNext()) {
                    keys.add(new String(cursor.next(), StandardCharsets.UTF_8));
                }
            }
            return null;
        });
        return keys;
    }

    /** 批量装配 VO：图片 / 卖家 / 分类各一次查询，消除列表 N+1。 */
    private List<ProductVO> buildVOList(List<Product> products, Long currentUserId) {
        if (products == null || products.isEmpty()) {
            return Collections.emptyList();
        }
        List<Long> productIds = products.stream().map(Product::getId).collect(Collectors.toList());
        List<Long> sellerIds = products.stream().map(Product::getSellerId)
                .filter(Objects::nonNull).distinct().collect(Collectors.toList());
        List<Long> catIds = products.stream().map(Product::getCategoryId)
                .filter(Objects::nonNull).distinct().collect(Collectors.toList());

        Map<Long, List<ProductImage>> imageMap = imageMapper.selectList(
                        new LambdaQueryWrapper<ProductImage>().in(ProductImage::getProductId, productIds))
                .stream().collect(Collectors.groupingBy(ProductImage::getProductId));
        imageMap.values().forEach(list -> list.sort(
                Comparator.comparing(ProductImage::getSortOrder,
                        Comparator.nullsLast(Comparator.naturalOrder()))));

        Map<Long, Map<String, Object>> sellerMap = sellerIds.isEmpty()
                ? Collections.emptyMap() : userDelegate.getPublicInfoMap(sellerIds);

        Map<Long, String> categoryMap = catIds.isEmpty()
                ? Collections.emptyMap()
                : categoryMapper.selectBatchIds(catIds).stream()
                        .collect(Collectors.toMap(Category::getId, Category::getName, (a, b) -> a));

        return products.stream()
                .map(p -> buildVO(p, currentUserId, imageMap, sellerMap, categoryMap))
                .collect(Collectors.toList());
    }

    private ProductVO buildVO(Product product, Long currentUserId) {
        return buildVOList(Collections.singletonList(product), currentUserId).get(0);
    }

    private ProductVO buildVO(Product product, Long currentUserId,
                              Map<Long, List<ProductImage>> imageMap,
                              Map<Long, Map<String, Object>> sellerMap,
                              Map<Long, String> categoryMap) {
        List<ProductImage> images = imageMap.getOrDefault(product.getId(), Collections.emptyList());

        Map<String, Object> info = sellerMap.get(product.getSellerId());
        String sellerName = "";
        String sellerAvatar = "";
        BigDecimal sellerReputation = BigDecimal.valueOf(5.0);
        if (info != null) {
            if (info.get("nickname") != null) sellerName = String.valueOf(info.get("nickname"));
            if (info.get("avatarUrl") != null) sellerAvatar = String.valueOf(info.get("avatarUrl"));
            Object rep = info.get("reputationScore");
            if (rep != null) sellerReputation = new BigDecimal(rep.toString());
        }

        String categoryName = categoryMap.getOrDefault(product.getCategoryId(), "");

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
                        .thumbUrl(img.getThumbUrl())
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



