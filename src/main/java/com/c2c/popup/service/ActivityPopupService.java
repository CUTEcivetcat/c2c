package com.c2c.popup.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.c2c.common.exception.BusinessException;
import com.c2c.coupon.entity.CouponTemplate;
import com.c2c.coupon.mapper.CouponTemplateMapper;
import com.c2c.popup.entity.ActivityPopup;
import com.c2c.popup.mapper.ActivityPopupMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 活动弹窗服务：用户端生效弹窗查询；管理端 CRUD 与上下架。
 * <p>绑定优惠券时，弹窗有效期跟随券有效期；券过期/下架则弹窗不再展示。</p>
 */
@Service
@RequiredArgsConstructor
public class ActivityPopupService {

    private final ActivityPopupMapper popupMapper;
    private final CouponTemplateMapper templateMapper;

    /** 用户端生效弹窗：已展示、在有效期内；绑券的额外校验券仍生效未过期 */
    public List<ActivityPopup> active() {
        LocalDateTime now = LocalDateTime.now();
        List<ActivityPopup> all = popupMapper.selectList(new LambdaQueryWrapper<ActivityPopup>()
                .eq(ActivityPopup::getStatus, 1)
                .and(q -> q.isNull(ActivityPopup::getValidStart).or().le(ActivityPopup::getValidStart, now))
                .and(q -> q.isNull(ActivityPopup::getValidEnd).or().ge(ActivityPopup::getValidEnd, now))
                .orderByDesc(ActivityPopup::getId));
        List<ActivityPopup> result = new ArrayList<>();
        for (ActivityPopup p : all) {
            if (isCouponAlive(p.getCouponTemplateId())) {
                result.add(p);
            }
        }
        return result;
    }

    /** 管理端全量弹窗（含下架），按 ID 倒序 */
    public Page<ActivityPopup> listAll(int page, int size, String keyword) {
        LambdaQueryWrapper<ActivityPopup> w = new LambdaQueryWrapper<ActivityPopup>()
                .orderByDesc(ActivityPopup::getId);
        if (keyword != null && !keyword.trim().isEmpty()) {
            w.like(ActivityPopup::getTitle, keyword.trim());
        }
        return popupMapper.selectPage(new Page<>(page, size), w);
    }

    /** 创建弹窗；绑定券时有效期跟随券 */
    @Transactional
    public ActivityPopup create(ActivityPopup popup, Long adminId) {
        checkRequired(popup);
        popup.setId(null);
        popup.setStatus(popup.getStatus() == null ? 1 : popup.getStatus());
        popup.setCreatedBy(adminId);
        applyCouponValidity(popup);
        popupMapper.insert(popup);
        return popup;
    }

    /** 编辑弹窗；绑定券时有效期跟随券 */
    @Transactional
    public ActivityPopup update(Long id, ActivityPopup p) {
        ActivityPopup exist = popupMapper.selectById(id);
        if (exist == null) {
            throw new BusinessException("弹窗不存在");
        }
        if (p.getTitle() != null) exist.setTitle(p.getTitle().trim());
        if (p.getContent() != null) exist.setContent(p.getContent());
        if (p.getImageUrl() != null) exist.setImageUrl(p.getImageUrl());
        if (p.getCouponTemplateId() != null) exist.setCouponTemplateId(p.getCouponTemplateId());
        if (p.getJumpType() != null) exist.setJumpType(p.getJumpType());
        if (p.getJumpTarget() != null) exist.setJumpTarget(p.getJumpTarget());
        if (p.getValidStart() != null) exist.setValidStart(p.getValidStart());
        if (p.getValidEnd() != null) exist.setValidEnd(p.getValidEnd());
        if (p.getStatus() != null) exist.setStatus(p.getStatus());
        applyCouponValidity(exist);
        popupMapper.updateById(exist);
        return exist;
    }

    /** 上架/下架（1 展示 0 下架） */
    public void changeStatus(Long id, Integer status) {
        ActivityPopup exist = popupMapper.selectById(id);
        if (exist == null) {
            throw new BusinessException("弹窗不存在");
        }
        exist.setStatus(status == null || status != 1 ? 0 : 1);
        popupMapper.updateById(exist);
    }

    public void delete(Long id) {
        popupMapper.deleteById(id);
    }

    /** 绑定券时：有效期截止跟随券 validEnd；券不存在则清空关联 */
    private void applyCouponValidity(ActivityPopup popup) {
        Long couponId = popup.getCouponTemplateId();
        if (couponId == null || couponId <= 0) {
            popup.setCouponTemplateId(0L);
            return;
        }
        CouponTemplate tpl = templateMapper.selectById(couponId);
        if (tpl == null) {
            throw new BusinessException("关联的优惠券不存在");
        }
        popup.setValidEnd(tpl.getValidEnd());
        if (popup.getValidStart() == null) {
            popup.setValidStart(tpl.getValidStart());
        }
    }

    /** 绑券弹窗校验券仍生效；未绑券直接通过 */
    private boolean isCouponAlive(Long couponTemplateId) {
        if (couponTemplateId == null || couponTemplateId <= 0) {
            return true;
        }
        CouponTemplate tpl = templateMapper.selectById(couponTemplateId);
        if (tpl == null || tpl.getStatus() == null || tpl.getStatus() != 1) {
            return false;
        }
        LocalDateTime now = LocalDateTime.now();
        if (tpl.getValidStart() != null && tpl.getValidStart().isAfter(now)) {
            return false;
        }
        return tpl.getValidEnd() == null || !tpl.getValidEnd().isBefore(now);
    }

    private void checkRequired(ActivityPopup popup) {
        if (popup.getTitle() == null || popup.getTitle().trim().isEmpty()) {
            throw new BusinessException("弹窗标题不能为空");
        }
        if (popup.getJumpType() == null || popup.getJumpType().trim().isEmpty()) {
            popup.setJumpType("none");
        }
    }
}
