package com.c2c.coupon.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.c2c.coupon.entity.CouponTemplate;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * 优惠券模板数据访问层。
 */
@Mapper
public interface CouponTemplateMapper extends BaseMapper<CouponTemplate> {

    /**
     * 原子占用一张券：仅当仍有库存且模板生效时才计数 +1，
     * 返回受影响行数（0 表示已抢光或已停用），避免并发超发。
     */
    @Update("UPDATE coupon_template SET claimed_count = claimed_count + 1 " +
            "WHERE id = #{id} AND claimed_count < total_count AND status = 1")
    int claimOne(@Param("id") Long id);
}
