package com.c2c.favorite.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.c2c.favorite.entity.Favorite;
import org.apache.ibatis.annotations.Mapper;

/**
 * 收藏数据访问层（MyBatis-Plus Mapper），负责对收藏记录表的增删改查操作。
 */
@Mapper
public interface FavoriteMapper extends BaseMapper<Favorite> {
}



