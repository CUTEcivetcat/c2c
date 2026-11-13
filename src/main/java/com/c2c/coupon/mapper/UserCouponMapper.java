package com.c2c.coupon.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.c2c.coupon.entity.UserCoupon;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户优惠券数据访问层。
 */
@Mapper
public interface UserCouponMapper extends BaseMapper<UserCoupon> {
}
