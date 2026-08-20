package com.c2c.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.c2c.order.entity.Order;
import org.apache.ibatis.annotations.Mapper;

/**
 * 订单数据访问层（MyBatis-Plus Mapper），负责对订单表的增删改查操作。
 */
@Mapper
public interface OrderMapper extends BaseMapper<Order> {
}



