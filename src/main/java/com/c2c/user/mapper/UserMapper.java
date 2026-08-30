package com.c2c.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.c2c.user.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.math.BigDecimal;

/**
 * 用户数据访问层（MyBatis-Plus Mapper），负责对用户表的增删改查操作。
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {

    /**
     * 原子扣减余额：仅当余额充足时才更新（WHERE balance &gt;= amount），
     * 返回受影响行数（0 表示余额不足），避免并发下读-改-写导致超扣。
     */
    @Update("UPDATE `user` SET balance = balance - #{amount} WHERE id = #{userId} AND balance >= #{amount}")
    int deductBalance(@Param("userId") Long userId, @Param("amount") BigDecimal amount);

    /**
     * 原子增加余额（充值/退款/收款）。
     */
    @Update("UPDATE `user` SET balance = balance + #{amount} WHERE id = #{userId}")
    int addBalance(@Param("userId") Long userId, @Param("amount") BigDecimal amount);
}
