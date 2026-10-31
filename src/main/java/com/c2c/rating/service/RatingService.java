package com.c2c.rating.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.c2c.common.exception.BusinessException;
import com.c2c.order.entity.Order;
import com.c2c.order.mapper.OrderMapper;
import com.c2c.rating.entity.Rating;
import com.c2c.rating.delegate.UserDelegate;
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
    private final UserDelegate userDelegate;
    private final OrderMapper orderMapper;

    /** 提交评价（校验订单参与/完成状态与评价方向，并重算被评用户信誉分） */
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

        validateOrderParticipation(rating, userId);

        Rating exist = ratingMapper.selectOne(new LambdaQueryWrapper<Rating>()
                .eq(Rating::getOrderId, rating.getOrderId())
                .eq(Rating::getRaterId, userId));
        if (exist != null) {
            throw new BusinessException(409, "该订单已评价");
        }

        ratingMapper.insert(rating);
        recalcReputation(rating.getRatedUserId());
    }

    /**
     * 订单参与校验：评价人必须是订单买家或卖家、订单必须已完成、评价角色与被评人必须与订单一致。
     * 防止对任意订单/任意用户刷评价、未完成订单提前评价。
     */
    private void validateOrderParticipation(Rating rating, Long userId) {
        Order order = orderMapper.selectById(rating.getOrderId());
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        boolean isBuyer = order.getBuyerId().equals(userId);
        boolean isSeller = order.getSellerId().equals(userId);
        if (!isBuyer && !isSeller) {
            throw new BusinessException("您不是该订单的买卖双方，无权评价");
        }
        if (order.getStatus() == null || order.getStatus() != 4) {
            throw new BusinessException("订单未完成，暂不能评价");
        }
        Long expectedRatedUserId = rating.getRole() == 1 ? order.getSellerId() : order.getBuyerId();
        if ((rating.getRole() == 1 && !isBuyer) || (rating.getRole() == 2 && !isSeller)) {
            throw new BusinessException("评价角色与订单身份不符（1=买家评卖家 2=卖家评买家）");
        }
        if (!expectedRatedUserId.equals(rating.getRatedUserId())) {
            throw new BusinessException("被评价用户与该订单不匹配");
        }
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
        userDelegate.updateReputation(ratedUserId,
                com.c2c.common.utils.MapUtils.of("reputationScore", score));
    }
}
