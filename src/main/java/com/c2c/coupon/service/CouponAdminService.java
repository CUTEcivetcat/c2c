package com.c2c.coupon.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.c2c.common.exception.BusinessException;
import com.c2c.coupon.entity.CouponTemplate;
import com.c2c.coupon.entity.UserCoupon;
import com.c2c.coupon.mapper.CouponTemplateMapper;
import com.c2c.coupon.mapper.UserCouponMapper;
import com.c2c.wallet.service.WalletService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 优惠券管理端服务：分页列表、创建、编辑、启停、删除。
 */
@Service
@RequiredArgsConstructor
public class CouponAdminService {

    private final CouponTemplateMapper templateMapper;
    private final UserCouponMapper userCouponMapper;
    private final WalletService walletService;

    /** 管理端全量券模板（含停用），按 ID 倒序 */
    public Page<CouponTemplate> listAll(int page, int size, String keyword) {
        LambdaQueryWrapper<CouponTemplate> w = new LambdaQueryWrapper<CouponTemplate>()
                .orderByDesc(CouponTemplate::getId);
        if (keyword != null && !keyword.trim().isEmpty()) {
            w.like(CouponTemplate::getName, keyword.trim());
        }
        return templateMapper.selectPage(new Page<>(page, size), w);
    }

    /** 创建券模板 */
    public CouponTemplate create(CouponTemplate t) {
        checkRequired(t);
        t.setId(null);
        t.setType(t.getType() == null ? CouponTemplate.TYPE_FULL_REDUCTION : t.getType());
        t.setStatus(t.getStatus() == null ? 1 : t.getStatus());
        t.setCategoryId(t.getCategoryId() == null ? 0 : t.getCategoryId());
        t.setMaxDiscountAmount(t.getMaxDiscountAmount() == null ? BigDecimal.ZERO : t.getMaxDiscountAmount());
        t.setClaimedCount(0);
        t.setName(t.getName().trim());
        templateMapper.insert(t);
        return t;
    }

    /** 编辑券模板（类型与已领取数不允许改动） */
    public CouponTemplate update(Long id, CouponTemplate t) {
        CouponTemplate exist = templateMapper.selectById(id);
        if (exist == null) {
            throw new BusinessException("优惠券不存在");
        }
        if (t.getName() != null) exist.setName(t.getName().trim());
        if (t.getDiscountAmount() != null) exist.setDiscountAmount(t.getDiscountAmount());
        if (t.getMinAmount() != null) exist.setMinAmount(t.getMinAmount());
        if (t.getCategoryId() != null) exist.setCategoryId(t.getCategoryId());
        if (t.getMaxDiscountAmount() != null) exist.setMaxDiscountAmount(t.getMaxDiscountAmount());
        if (t.getTotalCount() != null) exist.setTotalCount(t.getTotalCount());
        if (t.getValidStart() != null) exist.setValidStart(t.getValidStart());
        if (t.getValidEnd() != null) exist.setValidEnd(t.getValidEnd());
        if (t.getStatus() != null) exist.setStatus(t.getStatus());
        validateAmounts(exist);
        templateMapper.updateById(exist);
        return exist;
    }

    /** 启用/停用（1 启用 0 停用） */
    public void changeStatus(Long id, Integer status) {
        CouponTemplate exist = templateMapper.selectById(id);
        if (exist == null) {
            throw new BusinessException("优惠券不存在");
        }
        exist.setStatus(status == null || status != 1 ? 0 : 1);
        templateMapper.updateById(exist);
    }

    /** 删除券模板 */
    public void delete(Long id) {
        templateMapper.deleteById(id);
    }

    /** 定向分发余额券：选指定用户直接入账，无需领取；扣库存 + 加余额 + 券包留记录 */
    @Transactional
    public UserCoupon grantToUser(Long templateId, Long userId) {
        CouponTemplate tpl = templateMapper.selectById(templateId);
        if (tpl == null) {
            throw new BusinessException("优惠券不存在");
        }
        if (!Integer.valueOf(CouponTemplate.TYPE_BALANCE).equals(tpl.getType())) {
            throw new BusinessException("仅余额券支持定向分发");
        }
        if (userId == null) {
            throw new BusinessException("请选择发放用户");
        }
        Long exists = userCouponMapper.selectCount(new LambdaQueryWrapper<UserCoupon>()
                .eq(UserCoupon::getCouponId, templateId)
                .eq(UserCoupon::getUserId, userId));
        if (exists != null && exists > 0) {
            throw new BusinessException("该用户已持有此券");
        }
        int rows = templateMapper.claimOne(templateId);
        if (rows == 0) {
            throw new BusinessException("优惠券已被抢光或已停用");
        }

        LocalDateTime now = LocalDateTime.now();
        UserCoupon uc = new UserCoupon();
        uc.setCouponId(templateId);
        uc.setUserId(userId);
        uc.setStatus(CouponService.STATUS_USED);
        uc.setDiscountAmount(tpl.getDiscountAmount());
        uc.setReceiveTime(now);
        uc.setUseTime(now);
        uc.setExpireTime(tpl.getValidEnd());
        userCouponMapper.insert(uc);

        walletService.grantBonus(userId, tpl.getDiscountAmount(), null,
                "管理员发放余额券 ¥" + tpl.getDiscountAmount() + "（" + tpl.getName() + "）");
        return uc;
    }

    private void checkRequired(CouponTemplate t) {
        if (t.getName() == null || t.getName().trim().isEmpty()) {
            throw new BusinessException("券名称不能为空");
        }
        if (t.getType() == null) {
            t.setType(CouponTemplate.TYPE_FULL_REDUCTION);
        }
        if (t.getTotalCount() == null || t.getTotalCount() < 0) {
            throw new BusinessException("发行总量不能为负");
        }
        validateAmounts(t);
    }

    private void validateAmounts(CouponTemplate t) {
        if (t.getMinAmount() == null || t.getMinAmount().compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException("使用门槛不能为负");
        }
        BigDecimal d = t.getDiscountAmount();
        if (d == null) {
            throw new BusinessException("优惠力度必须填写");
        }
        if (Integer.valueOf(CouponTemplate.TYPE_DISCOUNT).equals(t.getType())) {
            if (d.compareTo(BigDecimal.ZERO) <= 0 || d.compareTo(BigDecimal.ONE) >= 0) {
                throw new BusinessException("折扣率需在 0~1 之间（如 0.9 表示 9 折）");
            }
        } else if (d.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("减免金额必须大于 0");
        }
        BigDecimal cap = t.getMaxDiscountAmount();
        if (cap != null && cap.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException("折扣封顶金额不能为负");
        }
    }
}
