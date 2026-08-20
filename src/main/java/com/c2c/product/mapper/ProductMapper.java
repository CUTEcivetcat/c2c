package com.c2c.product.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.c2c.product.entity.Product;
import org.apache.ibatis.annotations.Mapper;

/**
 * 商品数据访问层（MyBatis-Plus Mapper），负责对商品表的增删改查操作。
 */
@Mapper
public interface ProductMapper extends BaseMapper<Product> {
}



