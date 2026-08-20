package com.c2c.im.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.c2c.im.entity.Conversation;
import org.apache.ibatis.annotations.Mapper;

/**
 * 会话数据访问层（MyBatis-Plus Mapper），负责对 IM 会话表的增删改查操作。
 */
@Mapper
public interface ConversationMapper extends BaseMapper<Conversation> {
}



