package com.c2c.product.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.c2c.product.entity.Category;
import org.apache.ibatis.annotations.Mapper;

/**
 * 商品分类数据访问层（MyBatis-Plus Mapper），负责对商品分类表的增删改查操作。
 */
@Mapper
public interface CategoryMapper extends BaseMapper<Category> {
}



