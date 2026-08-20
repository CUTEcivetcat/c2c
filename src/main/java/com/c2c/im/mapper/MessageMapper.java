package com.c2c.im.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.c2c.im.entity.Message;
import org.apache.ibatis.annotations.Mapper;

/**
 * 消息数据访问层（MyBatis-Plus Mapper），负责对 IM 消息表的增删改查操作。
 */
@Mapper
public interface MessageMapper extends BaseMapper<Message> {
}



