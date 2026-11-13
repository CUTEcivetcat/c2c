package com.c2c.coupon.task;

import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.c2c.coupon.entity.UserCoupon;
import com.c2c.coupon.mapper.UserCouponMapper;
import com.c2c.coupon.service.CouponService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 优惠券过期兜底扫描任务。
 *
 * <p>用户券在「我的券」列表查询时会惰性过期；此处定时全量扫描，把已到期仍未使用的券批量置为过期，
 * 避免长期不进入券包的用户手中滞留无效未使用券。已使用/已作废券不受影响。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CouponExpireTask {

    private final UserCouponMapper userCouponMapper;

    /** 每 10 分钟扫描一次到期未使用券 */
    @Scheduled(fixedDelay = 600_000, initialDelay = 120_000)
    @Transactional
    public void expireDueCoupons() {
        LocalDateTime now = LocalDateTime.now();
        int rows = userCouponMapper.update(null, new UpdateWrapper<UserCoupon>()
                .eq("status", CouponService.STATUS_UNUSED)
                .isNotNull("expire_time")
                .lt("expire_time", now)
                .set("status", CouponService.STATUS_EXPIRED));
        if (rows > 0) {
            log.info("coupon expire scan: {} 张到期未使用券置为过期", rows);
        }
    }
}
