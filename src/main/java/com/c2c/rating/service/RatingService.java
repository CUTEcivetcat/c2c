package com.c2c.rating.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.c2c.common.exception.BusinessException;
import com.c2c.rating.entity.Rating;
import com.c2c.rating.feign.UserFeignClient;
import com.c2c.rating.mapper.RatingMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;

/**
 * 交易评价服务
 * <p>提供订单评价提交、用户评价分页查询、订单买卖双方双向评价查询能力，
 * 评价提交后自动重算被评用户的信誉分并同步到用户服务。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RatingService {

    private final RatingMapper ratingMapper;
    private final UserFeignClient userFeignClient;

    /** 提交评价（校验评分范围与重复评价，并重算被评用户信誉分） */
    @Transactional
    public void submit(Rating rating, Long userId) {
        rating.setRaterId(userId);
        if (rating.getScore() == null || rating.getScore() < 1 || rating.getScore() > 5) {
            throw new BusinessException("评分必须在1-5分之间");
        }
        if (rating.getOrderId() == null) {
            throw new BusinessException("订单ID不能为空");
        }
        if (rating.getRatedUserId() == null) {
            throw new BusinessException("被评价用户ID不能为空");
        }
        if (rating.getRole() == null || (rating.getRole() != 1 && rating.getRole() != 2)) {
            throw new BusinessException("评价角色无效（1=买家 2=卖家）");
        }

        Rating exist = ratingMapper.selectOne(new LambdaQueryWrapper<Rating>()
                .eq(Rating::getOrderId, rating.getOrderId())
                .eq(Rating::getRaterId, userId));
        if (exist != null) {
            throw new BusinessException(409, "该订单已评价");
        }

        ratingMapper.insert(rating);
        recalcReputation(rating.getRatedUserId());
    }

    /** 分页查询某用户收到的全部评价 */
    public Page<Rating> getUserRatings(Long userId, int page, int size) {
        return ratingMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<Rating>().eq(Rating::getRatedUserId, userId)
                        .orderByDesc(Rating::getCreatedAt));
    }

    /** 查询某订单的买家与卖家双向评价 */
    public Map<String, Object> getOrderRatings(Long orderId) {
        Rating buyer = ratingMapper.selectOne(new LambdaQueryWrapper<Rating>()
                .eq(Rating::getOrderId, orderId).eq(Rating::getRole, 1));
        Rating seller = ratingMapper.selectOne(new LambdaQueryWrapper<Rating>()
                .eq(Rating::getOrderId, orderId).eq(Rating::getRole, 2));
        return com.c2c.common.utils.MapUtils.of(
                "buyerRating", buyer == null ? "" : buyer,
                "sellerRating", seller == null ? "" : seller);
    }

    private void recalcReputation(Long ratedUserId) {
        Double avg = ratingMapper.selectList(new LambdaQueryWrapper<Rating>()
                .eq(Rating::getRatedUserId, ratedUserId))
                .stream()
                .mapToInt(Rating::getScore)
                .average()
                .orElse(5.0);
        BigDecimal score = BigDecimal.valueOf(Math.min(5.0, Math.max(1.0, avg)))
                .setScale(1, RoundingMode.HALF_UP);
        userFeignClient.updateReputation(ratedUserId,
                com.c2c.common.utils.MapUtils.of("reputationScore", score));
    }
}
