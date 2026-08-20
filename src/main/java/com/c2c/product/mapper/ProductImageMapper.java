package com.c2c.product.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.c2c.product.entity.ProductImage;
import org.apache.ibatis.annotations.Mapper;

/**
 * 商品图片数据访问层（MyBatis-Plus Mapper），负责对商品图片表的增删改查操作。
 */
@Mapper
public interface ProductImageMapper extends BaseMapper<ProductImage> {
}



