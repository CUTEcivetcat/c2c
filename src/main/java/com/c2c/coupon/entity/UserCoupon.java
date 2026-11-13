package com.c2c.coupon.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 用户已领优惠券：一条记录代表某用户持有某模板的一张券。
 * 状态：0 未使用 / 1 已使用 / 2 已过期 / 3 已作废 / 4 已锁定（待支付占用）。
 */
@Data
@TableName("user_coupon")
public class UserCoupon {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 券模板 ID */
    private Long couponId;

    /** 持有人 */
    private Long userId;

    /** 0 未使用 / 1 已使用 / 2 已过期 / 3 已作废 / 4 已锁定（待支付占用） */
    private Integer status;

    /** 使用后回填订单 ID */
    private Long orderId;

    /** 满减：减免金额快照；折扣：折扣率快照（抵扣额按订单金额实时计算） */
    private BigDecimal discountAmount;

    /** 领取时间 */
    private LocalDateTime receiveTime;

    /** 使用时间 */
    private LocalDateTime useTime;

    /** 过期时间 */
    private LocalDateTime expireTime;
}
