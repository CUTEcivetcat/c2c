package com.c2c.rating;

import com.c2c.common.exception.BusinessException;
import com.c2c.common.infra.TestLog;
import com.c2c.order.entity.Order;
import com.c2c.order.mapper.OrderMapper;
import com.c2c.rating.entity.Rating;
import com.c2c.rating.feign.UserFeignClient;
import com.c2c.rating.mapper.RatingMapper;
import com.c2c.rating.service.RatingService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 评价订单参与校验单元测试（S2/M1-1.3）。
 *
 * <p>校验规则：评价人必须是订单买家或卖家、订单必须已完成(status=4)、
 * 评价角色与被评人必须与订单身份一致（1=买家评卖家 2=卖家评买家）、每单每人限评一次。</p>
 */
@ExtendWith(MockitoExtension.class)
class RatingServiceTest {

    @Mock
    private RatingMapper ratingMapper;
    @Mock
    private UserFeignClient userFeignClient;
    @Mock
    private OrderMapper orderMapper;

    @InjectMocks
    private RatingService ratingService;

    @org.junit.jupiter.api.BeforeAll
    static void header() {
        TestLog.log("[测试类] 评价订单参与校验：正常互评 / 非订单双方 / 订单未完成 / 角色不符 / 被评人不符 / 重复评价 / 分数越界");
    }

    /** 已完成订单：买家=2，卖家=1 */
    private Order finishedOrder() {
        Order order = new Order();
        order.setId(10L);
        order.setBuyerId(2L);
        order.setSellerId(1L);
        order.setStatus(4);
        return order;
    }

    private Rating rating(Long raterId, int role, Long ratedUserId) {
        Rating rating = new Rating();
        rating.setOrderId(10L);
        rating.setRaterId(raterId);
        rating.setRole(role);
        rating.setRatedUserId(ratedUserId);
        rating.setScore(5);
        return rating;
    }

    @Test
    void submit_buyerRatesSeller_success() {
        TestLog.log("[测试] 正常路径：买家(role=1)评卖家 → 写入评价并触发信誉分重算");
        when(orderMapper.selectById(10L)).thenReturn(finishedOrder());
        when(ratingMapper.selectOne(any())).thenReturn(null); // 未评价过
        when(ratingMapper.selectList(any())).thenReturn(
                Collections.singletonList(rating(2L, 1, 1L)));

        ratingService.submit(rating(2L, 1, 1L), 2L);

        verify(ratingMapper).insert(any(Rating.class));
        verify(userFeignClient).updateReputation(eq(1L), any());
    }

    @Test
    void submit_notOrderParticipant_throws() {
        TestLog.log("[测试] 越权：路人(非本单买卖双方)提交评价应被拒绝");
        when(orderMapper.selectById(10L)).thenReturn(finishedOrder());

        assertThrows(BusinessException.class,
                () -> ratingService.submit(rating(99L, 1, 1L), 99L));
        verify(ratingMapper, never()).insert(any());
    }

    @Test
    void submit_unfinishedOrder_throws() {
        TestLog.log("[测试] 订单未完成(status=2已发货)：不允许提前评价");
        Order order = finishedOrder();
        order.setStatus(2);
        when(orderMapper.selectById(10L)).thenReturn(order);

        assertThrows(BusinessException.class,
                () -> ratingService.submit(rating(2L, 1, 1L), 2L));
        verify(ratingMapper, never()).insert(any());
    }

    @Test
    void submit_roleMismatch_throws() {
        TestLog.log("[测试] 角色不符：买家身份却提交「卖家评价」(role=2)应被拒绝");
        when(orderMapper.selectById(10L)).thenReturn(finishedOrder());

        assertThrows(BusinessException.class,
                () -> ratingService.submit(rating(2L, 2, 1L), 2L));
        verify(ratingMapper, never()).insert(any());
    }

    @Test
    void submit_wrongRatedUser_throws() {
        TestLog.log("[测试] 被评人不符：买家评价时把被评人填成自己应被拒绝");
        when(orderMapper.selectById(10L)).thenReturn(finishedOrder());

        assertThrows(BusinessException.class,
                () -> ratingService.submit(rating(2L, 1, 2L), 2L));
        verify(ratingMapper, never()).insert(any());
    }

    @Test
    void submit_duplicate_throws() {
        TestLog.log("[测试] 重复评价：同一评价人对同一订单再次评价返回409");
        when(orderMapper.selectById(10L)).thenReturn(finishedOrder());
        when(ratingMapper.selectOne(any())).thenReturn(rating(2L, 1, 1L)); // 已评价过

        BusinessException e = assertThrows(BusinessException.class,
                () -> ratingService.submit(rating(2L, 1, 1L), 2L));
        assertEquals(409, e.getCode());
        verify(ratingMapper, never()).insert(any());
    }

    @Test
    void submit_invalidScore_throws() {
        TestLog.log("[测试] 分数越界：评分6分(>5)应被拒绝，且不查询订单");
        Rating rating = rating(2L, 1, 1L);
        rating.setScore(6);
        assertThrows(BusinessException.class, () -> ratingService.submit(rating, 2L));
        verify(orderMapper, never()).selectById(any());
    }
}
