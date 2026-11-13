package com.c2c.coupon;

import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.c2c.common.exception.BusinessException;
import com.c2c.coupon.entity.CouponTemplate;
import com.c2c.coupon.entity.UserCoupon;
import com.c2c.coupon.mapper.CouponTemplateMapper;
import com.c2c.coupon.mapper.UserCouponMapper;
import com.c2c.coupon.service.CouponService;
import com.c2c.product.entity.Category;
import com.c2c.product.mapper.CategoryMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * 优惠券服务单元测试：领券防重复/防超发、抵扣门槛试算、支付锁定并发口径。
 *
 * <p>库存扣减走模板表原子 SQL（claimOne 受影响行数），重复领取靠唯一索引兜底；
 * 单测验证服务层判断与调用，最终落库由 SQL 保证。</p>
 */
@ExtendWith(MockitoExtension.class)
class CouponServiceTest {

    @Mock
    private CouponTemplateMapper templateMapper;
    @Mock
    private UserCouponMapper userCouponMapper;
    @Mock
    private CategoryMapper categoryMapper;
    @Mock
    private StringRedisTemplate redisTemplate;
    @Mock
    private ValueOperations<String, String> valueOps;

    @InjectMocks
    private CouponService couponService;

    @BeforeEach
    void mockClaimLock() {
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOps);
        lenient().when(valueOps.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(true);
        lenient().when(redisTemplate.execute(any(DefaultRedisScript.class), anyList(), anyString())).thenReturn(1L);
    }

    private CouponTemplate activeTemplate() {
        CouponTemplate t = new CouponTemplate();
        t.setId(1L);
        t.setStatus(1);
        t.setDiscountAmount(new BigDecimal("20"));
        t.setMinAmount(new BigDecimal("100"));
        t.setTotalCount(10);
        t.setClaimedCount(3);
        return t;
    }

    private UserCoupon unusedCoupon() {
        UserCoupon uc = new UserCoupon();
        uc.setId(5L);
        uc.setCouponId(1L);
        uc.setUserId(2L);
        uc.setStatus(CouponService.STATUS_UNUSED);
        uc.setDiscountAmount(new BigDecimal("20"));
        return uc;
    }

    @Test
    void claim_success_insertsUserCoupon() {
        when(templateMapper.selectById(1L)).thenReturn(activeTemplate());
        when(userCouponMapper.selectCount(any())).thenReturn(0L);
        when(templateMapper.claimOne(1L)).thenReturn(1);
        when(userCouponMapper.insert(any(UserCoupon.class))).thenReturn(1);

        UserCoupon uc = couponService.claim(1L, 2L);
        assertEquals(20, uc.getDiscountAmount().intValue());
        verify(userCouponMapper).insert(any(UserCoupon.class));
    }

    @Test
    void claim_alreadyClaimed_throwsWithoutDeduct() {
        when(templateMapper.selectById(1L)).thenReturn(activeTemplate());
        when(userCouponMapper.selectCount(any())).thenReturn(1L);

        assertThrows(BusinessException.class, () -> couponService.claim(1L, 2L));
        verify(templateMapper, never()).claimOne(any());
        verify(userCouponMapper, never()).insert(any());
    }

    @Test
    void claim_soldOut_throws() {
        when(templateMapper.selectById(1L)).thenReturn(activeTemplate());
        when(userCouponMapper.selectCount(any())).thenReturn(0L);
        when(templateMapper.claimOne(1L)).thenReturn(0);

        assertThrows(BusinessException.class, () -> couponService.claim(1L, 2L));
        verify(userCouponMapper, never()).insert(any());
    }

    @Test
    void claim_lockAcquireFails_throwsWithoutDeduct() {
        when(valueOps.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(false);

        assertThrows(BusinessException.class, () -> couponService.claim(1L, 2L));
        verify(templateMapper, never()).selectById(any());
        verify(templateMapper, never()).claimOne(any());
        verify(userCouponMapper, never()).insert(any());
    }

    @Test
    void calculate_meetsThreshold_returnsPayable() {
        when(userCouponMapper.selectById(5L)).thenReturn(unusedCoupon());
        when(templateMapper.selectById(1L)).thenReturn(activeTemplate());

        Map<String, Object> result = couponService.calculate(5L, 2L, new BigDecimal("150"), null);
        assertEquals(0, new BigDecimal("20").compareTo((BigDecimal) result.get("discountAmount")));
        assertEquals(0, new BigDecimal("130").compareTo((BigDecimal) result.get("payableAmount")));
    }

    @Test
    void calculate_belowThreshold_throws() {
        when(userCouponMapper.selectById(5L)).thenReturn(unusedCoupon());
        when(templateMapper.selectById(1L)).thenReturn(activeTemplate());

        assertThrows(BusinessException.class,
                () -> couponService.calculate(5L, 2L, new BigDecimal("80"), null));
    }

    @Test
    void calculate_discountType_appliesRate() {
        CouponTemplate discountTpl = activeTemplate();
        discountTpl.setType(CouponTemplate.TYPE_DISCOUNT);
        discountTpl.setDiscountAmount(new BigDecimal("0.9"));
        discountTpl.setMinAmount(BigDecimal.ZERO);
        when(userCouponMapper.selectById(5L)).thenReturn(unusedCoupon());
        when(templateMapper.selectById(1L)).thenReturn(discountTpl);

        Map<String, Object> result = couponService.calculate(5L, 2L, new BigDecimal("150"), null);
        assertEquals(0, new BigDecimal("15.00").compareTo((BigDecimal) result.get("discountAmount")));
        assertEquals(0, new BigDecimal("135.00").compareTo((BigDecimal) result.get("payableAmount")));
    }

    @Test
    void calculate_categoryMismatch_throws() {
        CouponTemplate tpl = activeTemplate();
        tpl.setCategoryId(10L);
        when(userCouponMapper.selectById(5L)).thenReturn(unusedCoupon());
        when(templateMapper.selectById(1L)).thenReturn(tpl);

        assertThrows(BusinessException.class,
                () -> couponService.calculate(5L, 2L, new BigDecimal("150"), 20L));
    }

    @Test
    void calculate_categoryMatch_orUniversal_ok() {
        CouponTemplate tpl = activeTemplate();
        tpl.setCategoryId(10L);
        when(userCouponMapper.selectById(5L)).thenReturn(unusedCoupon());
        when(templateMapper.selectById(1L)).thenReturn(tpl);
        // 同分类可用
        Map<String, Object> same = couponService.calculate(5L, 2L, new BigDecimal("150"), 10L);
        assertEquals(0, new BigDecimal("130").compareTo((BigDecimal) same.get("payableAmount")));

        CouponTemplate universal = activeTemplate();
        universal.setCategoryId(0L);
        when(templateMapper.selectById(1L)).thenReturn(universal);
        // 通用券任意分类可用
        Map<String, Object> any = couponService.calculate(5L, 2L, new BigDecimal("150"), 20L);
        assertEquals(0, new BigDecimal("130").compareTo((BigDecimal) any.get("payableAmount")));
    }

    @Test
    void markUsed_alreadyUsed_throws() {
        when(userCouponMapper.selectById(5L)).thenReturn(unusedCoupon());
        when(userCouponMapper.update(any(), any(UpdateWrapper.class))).thenReturn(0);

        assertThrows(BusinessException.class, () -> couponService.markUsed(5L, 99L, 2L));
    }

    private UserCoupon usedCoupon(LocalDateTime expireTime) {
        UserCoupon uc = unusedCoupon();
        uc.setStatus(CouponService.STATUS_USED);
        uc.setOrderId(99L);
        uc.setExpireTime(expireTime);
        return uc;
    }

    @Test
    void releaseAfterCancel_usedNotExpired_backToUnused() {
        when(userCouponMapper.selectById(5L)).thenReturn(usedCoupon(LocalDateTime.now().plusDays(1)));
        when(userCouponMapper.update(any(), any(UpdateWrapper.class))).thenReturn(1);

        couponService.releaseAfterCancel(5L, 99L);
        verify(userCouponMapper).update(any(), any(UpdateWrapper.class));
    }

    @Test
    void releaseAfterCancel_usedExpired_setExpired() {
        when(userCouponMapper.selectById(5L)).thenReturn(usedCoupon(LocalDateTime.now().minusDays(1)));
        when(userCouponMapper.update(any(), any(UpdateWrapper.class))).thenReturn(1);

        couponService.releaseAfterCancel(5L, 99L);
        verify(userCouponMapper).update(any(), any(UpdateWrapper.class));
    }

    @Test
    void releaseAfterCancel_notUsed_noUpdate() {
        when(userCouponMapper.selectById(5L)).thenReturn(unusedCoupon());

        couponService.releaseAfterCancel(5L, 99L);
        verify(userCouponMapper, never()).update(any(), any(UpdateWrapper.class));
    }

    @Test
    void available_guestNullUser_doesNotQueryUserCoupons() {
        CouponTemplate tpl = activeTemplate();
        Page<CouponTemplate> tplPage = new Page<>(1, 10, 1);
        tplPage.setRecords(Collections.singletonList(tpl));
        when(templateMapper.selectPage(any(Page.class), any())).thenReturn(tplPage);

        Page<Map<String, Object>> page = couponService.available(null, 1, 10, null);

        assertEquals(1, page.getRecords().size());
        assertEquals(false, page.getRecords().get(0).get("claimed"));
        verify(userCouponMapper, never()).selectList(any());
    }

    @Test
    void myCoupons_empty_returnsEmptyWithoutBatchQuery() {
        when(userCouponMapper.selectList(any())).thenReturn(Collections.emptyList());

        List<Map<String, Object>> rows = couponService.myCoupons(2L, 0);

        assertTrue(rows.isEmpty());
        verify(templateMapper, never()).selectBatchIds(any());
        verify(categoryMapper, never()).selectBatchIds(any());
    }

    @Test
    void releaseAfterCancel_orderMismatch_noUpdate() {
        UserCoupon uc = usedCoupon(LocalDateTime.now().plusDays(1));
        uc.setOrderId(88L);
        when(userCouponMapper.selectById(5L)).thenReturn(uc);

        couponService.releaseAfterCancel(5L, 99L);
        verify(userCouponMapper, never()).update(any(), any(UpdateWrapper.class));
    }

    private Category category(Long id, Long parentId) {
        Category c = new Category();
        c.setId(id);
        c.setParentId(parentId);
        return c;
    }

    @Test
    void calculate_childCategory_parentCoupon_ok() {
        CouponTemplate tpl = activeTemplate();
        tpl.setCategoryId(10L);
        when(userCouponMapper.selectById(5L)).thenReturn(unusedCoupon());
        when(templateMapper.selectById(1L)).thenReturn(tpl);
        when(categoryMapper.selectById(20L)).thenReturn(category(20L, 10L));
        when(categoryMapper.selectById(10L)).thenReturn(category(10L, 0L));

        // 券绑父分类 10，商品属子分类 20，向上递归匹配可用
        Map<String, Object> result = couponService.calculate(5L, 2L, new BigDecimal("150"), 20L);
        assertEquals(0, new BigDecimal("130").compareTo((BigDecimal) result.get("payableAmount")));
    }

    @Test
    void calculate_discount_cappedByMaxDiscount() {
        CouponTemplate discountTpl = activeTemplate();
        discountTpl.setType(CouponTemplate.TYPE_DISCOUNT);
        discountTpl.setDiscountAmount(new BigDecimal("0.9"));
        discountTpl.setMinAmount(BigDecimal.ZERO);
        discountTpl.setMaxDiscountAmount(new BigDecimal("10"));
        when(userCouponMapper.selectById(5L)).thenReturn(unusedCoupon());
        when(templateMapper.selectById(1L)).thenReturn(discountTpl);

        // 订单 150，9 折减免 15，封顶 10 → 实减 10
        Map<String, Object> result = couponService.calculate(5L, 2L, new BigDecimal("150"), null);
        assertEquals(0, new BigDecimal("10.00").compareTo((BigDecimal) result.get("discountAmount")));
        assertEquals(0, new BigDecimal("140.00").compareTo((BigDecimal) result.get("payableAmount")));
    }

    @Test
    void reserve_success_marksReserved() {
        when(userCouponMapper.selectById(5L)).thenReturn(unusedCoupon());
        when(userCouponMapper.update(any(), any(UpdateWrapper.class))).thenReturn(1);

        couponService.reserve(5L, 99L, 2L);
        verify(userCouponMapper).update(any(), any(UpdateWrapper.class));
    }

    @Test
    void reserve_alreadyReserved_throws() {
        when(userCouponMapper.selectById(5L)).thenReturn(unusedCoupon());
        when(userCouponMapper.update(any(), any(UpdateWrapper.class))).thenReturn(0);

        assertThrows(BusinessException.class, () -> couponService.reserve(5L, 99L, 2L));
    }

    @Test
    void markUsed_reserved_ok() {
        UserCoupon reserved = unusedCoupon();
        reserved.setStatus(CouponService.STATUS_RESERVED);
        reserved.setOrderId(99L);
        when(userCouponMapper.selectById(5L)).thenReturn(reserved);
        when(userCouponMapper.update(any(), any(UpdateWrapper.class))).thenReturn(1);

        assertDoesNotThrow(() -> couponService.markUsed(5L, 99L, 2L));
    }

    @Test
    void releaseAfterCancel_reserved_backToUnused() {
        UserCoupon reserved = unusedCoupon();
        reserved.setStatus(CouponService.STATUS_RESERVED);
        reserved.setOrderId(99L);
        when(userCouponMapper.selectById(5L)).thenReturn(reserved);
        when(userCouponMapper.update(any(), any(UpdateWrapper.class))).thenReturn(1);

        couponService.releaseAfterCancel(5L, 99L);
        verify(userCouponMapper).update(any(), any(UpdateWrapper.class));
    }
}
