package com.c2c.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.c2c.user.entity.User;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户数据访问层（MyBatis-Plus Mapper），负责对用户表的增删改查操作。
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {
}



