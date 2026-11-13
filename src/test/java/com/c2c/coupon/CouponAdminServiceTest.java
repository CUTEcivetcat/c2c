package com.c2c.coupon;

import com.c2c.common.exception.BusinessException;
import com.c2c.common.infra.TestLog;
import com.c2c.coupon.entity.CouponTemplate;
import com.c2c.coupon.mapper.CouponTemplateMapper;
import com.c2c.coupon.mapper.UserCouponMapper;
import com.c2c.coupon.service.CouponAdminService;
import com.c2c.wallet.service.WalletService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * 优惠券管理端服务单元测试：建券校验（满减/折扣）、编辑保留类型、启停与不存在分支。
 *
 * <p>折扣率须在 0~1 之间；满减减免额须大于 0；门槛与总量不能为负。</p>
 *
 * @author wzh
 */
@ExtendWith(MockitoExtension.class)
class CouponAdminServiceTest {

    @Mock
    private CouponTemplateMapper templateMapper;
    @Mock
    private UserCouponMapper userCouponMapper;
    @Mock
    private WalletService walletService;

    @InjectMocks
    private CouponAdminService couponAdminService;

    private CouponTemplate fullReduction(BigDecimal discount, BigDecimal minAmount, Integer total) {
        CouponTemplate t = new CouponTemplate();
        t.setName("测试满减券");
        t.setType(CouponTemplate.TYPE_FULL_REDUCTION);
        t.setDiscountAmount(discount);
        t.setMinAmount(minAmount);
        t.setTotalCount(total);
        t.setStatus(1);
        return t;
    }

    private CouponTemplate discount(BigDecimal rate, BigDecimal minAmount, Integer total) {
        CouponTemplate t = new CouponTemplate();
        t.setName("测试折扣券");
        t.setType(CouponTemplate.TYPE_DISCOUNT);
        t.setDiscountAmount(rate);
        t.setMinAmount(minAmount);
        t.setTotalCount(total);
        t.setStatus(1);
        return t;
    }

    @Test
    void create_emptyName_throws() {
        TestLog.log("[测试] 建券校验：空白名称应被拒绝");
        CouponTemplate t = fullReduction(new BigDecimal("10"), new BigDecimal("50"), 5);
        t.setName("   ");
        assertThrows(BusinessException.class, () -> couponAdminService.create(t));
        verify(templateMapper, never()).insert(any());
    }

    @Test
    void create_fullReduction_appliesDefaults() {
        TestLog.log("[测试] 建券：满减券类型/状态/已领数默认值归一");
        CouponTemplate t = fullReduction(new BigDecimal("10"), new BigDecimal("50"), 5);
        t.setType(null);
        t.setStatus(null);
        when(templateMapper.insert(any())).thenReturn(1);

        CouponTemplate created = couponAdminService.create(t);

        assertEquals(CouponTemplate.TYPE_FULL_REDUCTION, created.getType());
        assertEquals(1, created.getStatus());
        assertEquals(0, created.getClaimedCount());
        verify(templateMapper).insert(t);
    }

    @Test
    void create_bindsCategory() {
        TestLog.log("[测试] 建券：绑定适用分类落库，未传则默认通用 0");
        CouponTemplate t = fullReduction(new BigDecimal("10"), new BigDecimal("50"), 5);
        t.setCategoryId(10L);
        when(templateMapper.insert(any())).thenReturn(1);

        CouponTemplate created = couponAdminService.create(t);
        assertEquals(10L, created.getCategoryId());

        CouponTemplate universal = fullReduction(new BigDecimal("10"), new BigDecimal("50"), 5);
        CouponTemplate u = couponAdminService.create(universal);
        assertEquals(0L, u.getCategoryId());
    }

    @Test
    void update_categoryChanged() {
        TestLog.log("[测试] 编辑保存：适用分类可更新");
        CouponTemplate exist = fullReduction(new BigDecimal("10"), new BigDecimal("50"), 5);
        exist.setId(1L);
        exist.setCategoryId(0L);
        when(templateMapper.selectById(1L)).thenReturn(exist);

        CouponTemplate edit = new CouponTemplate();
        edit.setCategoryId(10L);
        couponAdminService.update(1L, edit);

        assertEquals(10L, exist.getCategoryId());
        verify(templateMapper).updateById(exist);
    }

    @Test
    void create_discount_success() {
        TestLog.log("[测试] 建券：折扣券 0.85（8.5 折）可正常落库");
        CouponTemplate t = discount(new BigDecimal("0.85"), BigDecimal.ZERO, 5);
        when(templateMapper.insert(any())).thenReturn(1);

        CouponTemplate created = couponAdminService.create(t);

        assertEquals(CouponTemplate.TYPE_DISCOUNT, created.getType());
        assertEquals(0, new BigDecimal("0.85").compareTo(created.getDiscountAmount()));
        verify(templateMapper).insert(t);
    }

    @Test
    void create_discountRateTooHigh_throws() {
        TestLog.log("[测试] 建券校验：折扣率 ≥1 应被拒绝");
        CouponTemplate t = discount(new BigDecimal("1.5"), BigDecimal.ZERO, 5);
        assertThrows(BusinessException.class, () -> couponAdminService.create(t));
        verify(templateMapper, never()).insert(any());
    }

    @Test
    void create_discountRateZero_throws() {
        TestLog.log("[测试] 建券校验：折扣率 ≤0 应被拒绝");
        CouponTemplate t = discount(BigDecimal.ZERO, BigDecimal.ZERO, 5);
        assertThrows(BusinessException.class, () -> couponAdminService.create(t));
        verify(templateMapper, never()).insert(any());
    }

    @Test
    void create_fullReductionNonPositive_throws() {
        TestLog.log("[测试] 建券校验：满减减免额 ≤0 应被拒绝");
        CouponTemplate t = fullReduction(BigDecimal.ZERO, new BigDecimal("50"), 5);
        assertThrows(BusinessException.class, () -> couponAdminService.create(t));
        verify(templateMapper, never()).insert(any());
    }

    @Test
    void create_negativeMinAmount_throws() {
        TestLog.log("[测试] 建券校验：使用门槛为负应被拒绝");
        CouponTemplate t = fullReduction(new BigDecimal("10"), new BigDecimal("-1"), 5);
        assertThrows(BusinessException.class, () -> couponAdminService.create(t));
        verify(templateMapper, never()).insert(any());
    }

    @Test
    void create_negativeTotalCount_throws() {
        TestLog.log("[测试] 建券校验：发行总量为负应被拒绝");
        CouponTemplate t = fullReduction(new BigDecimal("10"), new BigDecimal("50"), -1);
        assertThrows(BusinessException.class, () -> couponAdminService.create(t));
        verify(templateMapper, never()).insert(any());
    }

    @Test
    void update_notFound_throws() {
        TestLog.log("[测试] 编辑校验：券不存在(99)时应抛异常");
        when(templateMapper.selectById(99L)).thenReturn(null);
        assertThrows(BusinessException.class, () -> couponAdminService.update(99L, new CouponTemplate()));
    }

    @Test
    void update_keepsOriginalType() {
        TestLog.log("[测试] 编辑保存：类型不可改，仍保持原满减类型");
        CouponTemplate exist = fullReduction(new BigDecimal("10"), new BigDecimal("50"), 5);
        exist.setId(1L);
        when(templateMapper.selectById(1L)).thenReturn(exist);

        CouponTemplate edit = new CouponTemplate();
        edit.setName("改后名称");
        edit.setDiscountAmount(new BigDecimal("20"));
        couponAdminService.update(1L, edit);

        assertEquals(CouponTemplate.TYPE_FULL_REDUCTION, exist.getType());
        assertEquals("改后名称", exist.getName());
        verify(templateMapper).updateById(exist);
    }

    @Test
    void update_discountRateInvalid_throws() {
        TestLog.log("[测试] 编辑校验：折扣券力度改成 ≥1 应被拒绝");
        CouponTemplate exist = discount(new BigDecimal("0.9"), BigDecimal.ZERO, 5);
        exist.setId(2L);
        when(templateMapper.selectById(2L)).thenReturn(exist);

        CouponTemplate edit = new CouponTemplate();
        edit.setDiscountAmount(new BigDecimal("1.5"));
        assertThrows(BusinessException.class, () -> couponAdminService.update(2L, edit));
        verify(templateMapper, never()).updateById(any());
    }

    @Test
    void changeStatus_notFound_throws() {
        TestLog.log("[测试] 启停校验：券不存在(99)时应抛异常");
        when(templateMapper.selectById(99L)).thenReturn(null);
        assertThrows(BusinessException.class, () -> couponAdminService.changeStatus(99L, 1));
    }

    @Test
    void changeStatus_disable() {
        TestLog.log("[测试] 启停：停用后置 status=0");
        CouponTemplate exist = fullReduction(new BigDecimal("10"), new BigDecimal("50"), 5);
        exist.setId(1L);
        when(templateMapper.selectById(1L)).thenReturn(exist);

        couponAdminService.changeStatus(1L, 0);

        assertEquals(0, exist.getStatus());
        verify(templateMapper).updateById(exist);
    }

    @Test
    void grant_nonBalanceCoupon_throws() {
        TestLog.log("[测试] 定向分发：仅余额券可直发，满减券应被拒绝，不扣库存不写券记录");
        CouponTemplate t = fullReduction(new BigDecimal("10"), new BigDecimal("50"), 5);
        t.setId(1L);
        when(templateMapper.selectById(1L)).thenReturn(t);
        assertThrows(BusinessException.class, () -> couponAdminService.grantToUser(1L, 2L));
        verify(templateMapper, never()).claimOne(any());
        verify(userCouponMapper, never()).insert(any());
    }
}
