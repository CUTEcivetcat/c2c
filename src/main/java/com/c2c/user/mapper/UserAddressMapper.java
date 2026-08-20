package com.c2c.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.c2c.user.entity.UserAddress;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户收货地址数据访问层（MyBatis-Plus Mapper），负责对用户收货地址表的增删改查操作。
 */
@Mapper
public interface UserAddressMapper extends BaseMapper<UserAddress> {
}



