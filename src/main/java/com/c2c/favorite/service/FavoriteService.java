package com.c2c.favorite.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.c2c.common.exception.BusinessException;
import com.c2c.favorite.entity.Favorite;
import com.c2c.favorite.mapper.FavoriteMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 商品收藏服务
 * <p>提供商品收藏、取消收藏、收藏列表分页查询及收藏状态判断能力，
 * 使用 Redis 缓存收藏标记以提升查询性能。</p>
 */
@Service
@RequiredArgsConstructor
public class FavoriteService {

    private final FavoriteMapper favoriteMapper;
    private final StringRedisTemplate redisTemplate;

    /** 收藏商品（重复收藏会抛出业务异常） */
    @Transactional
    public void add(Long userId, Long productId) {
        if (favoriteMapper.selectCount(new LambdaQueryWrapper<Favorite>()
                .eq(Favorite::getUserId, userId).eq(Favorite::getProductId, productId)) > 0) {
            throw new BusinessException(409, "您已收藏过该商品");
        }
        Favorite f = new Favorite();
        f.setUserId(userId);
        f.setProductId(productId);
        favoriteMapper.insert(f);
        redisTemplate.opsForValue().set("favorite:check:" + userId + ":" + productId, "1", 24, TimeUnit.HOURS);
    }

    /** 取消收藏商品 */
    @Transactional
    public void remove(Long userId, Long productId) {
        favoriteMapper.delete(new LambdaQueryWrapper<Favorite>()
                .eq(Favorite::getUserId, userId).eq(Favorite::getProductId, productId));
        redisTemplate.delete("favorite:check:" + userId + ":" + productId);
    }

    /** 分页查询用户的收藏列表 */
    public Page<Favorite> list(Long userId, int page, int size) {
        return favoriteMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<Favorite>().eq(Favorite::getUserId, userId).orderByDesc(Favorite::getCreatedAt));
    }

    /** 判断用户是否已收藏指定商品（带 Redis 缓存） */
    public boolean isFavorited(Long userId, Long productId) {
        String val = redisTemplate.opsForValue().get("favorite:check:" + userId + ":" + productId);
        if (val != null) return "1".equals(val);
        boolean fav = favoriteMapper.selectCount(new LambdaQueryWrapper<Favorite>()
                .eq(Favorite::getUserId, userId).eq(Favorite::getProductId, productId)) > 0;
        redisTemplate.opsForValue().set("favorite:check:" + userId + ":" + productId, fav ? "1" : "0", 1, TimeUnit.HOURS);
        return fav;
    }
}



