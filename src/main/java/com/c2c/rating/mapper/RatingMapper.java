package com.c2c.rating.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.c2c.rating.entity.Rating;
import org.apache.ibatis.annotations.Mapper;

/**
 * 评价数据访问层（MyBatis-Plus Mapper），负责对评价表的增删改查操作。
 */
@Mapper
public interface RatingMapper extends BaseMapper<Rating> {
}



