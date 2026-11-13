package com.c2c.coupon.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 优惠券模板（发行批次）：管理员创建，用户领取后生成 {@link UserCoupon}。
 */
@Data
@TableName("coupon_template")
public class CouponTemplate {

    /** 满减券：满门槛减定额 */
    public static final int TYPE_FULL_REDUCTION = 1;
    /** 折扣券：按折扣率打折 */
    public static final int TYPE_DISCOUNT = 2;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 券名称 */
    private String name;

    /** 券类型：1 满减 2 折扣 */
    private Integer type;

    /** 满减：减免金额；折扣：折扣率（0~1，如 0.9 表示 9 折） */
    private BigDecimal discountAmount;

    /** 折扣券减免封顶金额，0 表示不封顶（仅折扣券生效） */
    private BigDecimal maxDiscountAmount;

    /** 使用门槛（订单金额满此额可用），0 无门槛 */
    private BigDecimal minAmount;

    /** 适用分类 ID（category.id），0 表示全部分类通用 */
    private Long categoryId;

    /** 发行总量 */
    private Integer totalCount;

    /** 已领取数 */
    private Integer claimedCount;

    /** 生效开始 */
    private LocalDateTime validStart;

    /** 有效期截止 */
    private LocalDateTime validEnd;

    /** 1 生效 0 停用 */
    private Integer status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
