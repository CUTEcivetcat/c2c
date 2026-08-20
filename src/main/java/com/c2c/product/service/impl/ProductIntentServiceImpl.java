package com.c2c.product.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.c2c.common.enums.IntentStatus;
import com.c2c.common.enums.ProductStatus;
import com.c2c.common.exception.BusinessException;
import com.c2c.product.dto.IntentCreateDTO;
import com.c2c.product.dto.IntentVO;
import com.c2c.product.entity.Product;
import com.c2c.product.entity.ProductImage;
import com.c2c.product.entity.ProductIntent;
import com.c2c.product.feign.UserFeignClient;
import com.c2c.product.mapper.ProductImageMapper;
import com.c2c.product.mapper.ProductIntentMapper;
import com.c2c.product.mapper.ProductMapper;
import com.c2c.product.service.ProductIntentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 购买意向服务实现类
 * <p>实现购买意向的创建、买卖双方列表查询、卖家回复、关闭与成交，
 * 通过 Feign 获取买家公开信息，并附带商品标题、封面与价格。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProductIntentServiceImpl implements ProductIntentService {

    private static final int MAX_LENGTH = 500;

    private final ProductIntentMapper intentMapper;
    private final ProductMapper productMapper;
    private final ProductImageMapper imageMapper;
    private final UserFeignClient userFeignClient;

    @Override
    public Long create(Long productId, IntentCreateDTO dto, Long buyerId) {
        Product product = productMapper.selectById(productId);
        if (product == null || product.getStatus() != ProductStatus.ON_SALE.getCode()) {
            throw new BusinessException("商品不存在或已下架");
        }
        if (product.getSellerId().equals(buyerId)) {
            throw new BusinessException("不能对自己的商品表达意向");
        }

        Long dup = intentMapper.selectCount(new LambdaQueryWrapper<ProductIntent>()
                .eq(ProductIntent::getProductId, productId)
                .eq(ProductIntent::getBuyerId, buyerId)
                .eq(ProductIntent::getStatus, IntentStatus.PENDING.getCode()));
        if (dup > 0) {
            throw new BusinessException("您已对该商品表达过意向，请等待卖家回复");
        }

        String message = dto.getMessage() == null ? null : dto.getMessage().trim();
        if (StrUtil.isNotBlank(message) && message.length() > MAX_LENGTH) {
            throw new BusinessException("留言不能超过" + MAX_LENGTH + "字");
        }

        ProductIntent intent = new ProductIntent();
        intent.setProductId(productId);
        intent.setSellerId(product.getSellerId());
        intent.setBuyerId(buyerId);
        intent.setMessage(message);
        intent.setExpectedPrice(dto.getExpectedPrice());
        intent.setStatus(IntentStatus.PENDING.getCode());
        intentMapper.insert(intent);
        return intent.getId();
    }

    @Override
    public Page<IntentVO> myList(Long buyerId, int page, int size) {
        Page<ProductIntent> result = intentMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<ProductIntent>()
                        .eq(ProductIntent::getBuyerId, buyerId)
                        .orderByDesc(ProductIntent::getCreatedAt));

        Page<IntentVO> voPage = new Page<>(page, size, result.getTotal());
        voPage.setRecords(result.getRecords().stream()
                .map(this::buildVO)
                .collect(Collectors.toList()));
        return voPage;
    }

    @Override
    public Page<IntentVO> sellerList(Long sellerId, int page, int size) {
        Page<ProductIntent> result = intentMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<ProductIntent>()
                        .eq(ProductIntent::getSellerId, sellerId)
                        .orderByDesc(ProductIntent::getCreatedAt));

        Page<IntentVO> voPage = new Page<>(page, size, result.getTotal());
        voPage.setRecords(result.getRecords().stream()
                .map(this::buildVO)
                .collect(Collectors.toList()));
        return voPage;
    }

    @Override
    public void reply(Long intentId, String reply, Long sellerId) {
        ProductIntent intent = requireSellerOwned(intentId, sellerId);
        intent.setSellerReply(reply == null ? "" : reply.trim());
        intent.setStatus(IntentStatus.REPLIED.getCode());
        intentMapper.updateById(intent);
    }

    @Override
    public void close(Long intentId, Long userId) {
        ProductIntent intent = intentMapper.selectById(intentId);
        if (intent == null
                || (!intent.getBuyerId().equals(userId) && !intent.getSellerId().equals(userId))) {
            throw new BusinessException("无权操作该意向");
        }
        intent.setStatus(IntentStatus.CLOSED.getCode());
        intentMapper.updateById(intent);
    }

    @Override
    public void deal(Long intentId, Long sellerId) {
        ProductIntent intent = requireSellerOwned(intentId, sellerId);
        intent.setStatus(IntentStatus.DEAL.getCode());
        intentMapper.updateById(intent);
    }

    private ProductIntent requireSellerOwned(Long intentId, Long sellerId) {
        ProductIntent intent = intentMapper.selectById(intentId);
        if (intent == null || !intent.getSellerId().equals(sellerId)) {
            throw new BusinessException("无权操作该意向");
        }
        return intent;
    }

    private IntentVO buildVO(ProductIntent intent) {
        Product product = productMapper.selectById(intent.getProductId());
        String title = product != null ? product.getTitle() : "";
        String cover = "";
        BigDecimal productPrice = product != null ? product.getPrice() : null;
        if (product != null) {
            List<ProductImage> images = imageMapper.selectList(new LambdaQueryWrapper<ProductImage>()
                    .eq(ProductImage::getProductId, product.getId())
                    .orderByAsc(ProductImage::getSortOrder));
            if (!images.isEmpty()) {
                cover = images.get(0).getUrl();
            }
        }

        String nickname = "";
        String avatar = "";
        try {
            Map<String, Object> userInfo = userFeignClient.getUserPublicInfo(intent.getBuyerId());
            if (userInfo != null && userInfo.get("data") instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> data = (Map<String, Object>) userInfo.get("data");
                nickname = String.valueOf(data.getOrDefault("nickname", ""));
                avatar = String.valueOf(data.getOrDefault("avatarUrl", ""));
            }
        } catch (Exception e) {
            log.warn("获取意向买家信息失败: buyerId={}", intent.getBuyerId());
        }

        return IntentVO.builder()
                .id(intent.getId())
                .productId(intent.getProductId())
                .productTitle(title)
                .productCover(cover)
                .productPrice(productPrice)
                .sellerId(intent.getSellerId())
                .buyerId(intent.getBuyerId())
                .buyerNickname(nickname)
                .buyerAvatar(avatar)
                .message(intent.getMessage())
                .expectedPrice(intent.getExpectedPrice())
                .status(intent.getStatus())
                .statusText(IntentStatus.getTextByCode(intent.getStatus()))
                .sellerReply(intent.getSellerReply())
                .createdAt(intent.getCreatedAt())
                .build();
    }
}
