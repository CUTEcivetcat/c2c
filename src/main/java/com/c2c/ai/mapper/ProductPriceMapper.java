package com.c2c.ai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.c2c.product.entity.Product;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Map;

/**
 * AI 估价数据源：同分类已售商品的历史成交均价（只读复用 product 表，不改现有模块）。
 */
@Mapper
public interface ProductPriceMapper extends BaseMapper<Product> {

    /**
     * 查询同分类已售商品（status=3）的历史成交均价与笔数。
     *
     * @return 含 {@code avgPrice}(BigDecimal) 与 {@code cnt}(Long) 的 Map；无数据返回 null
     */
    @Select("SELECT AVG(price) AS avgPrice, COUNT(*) AS cnt " +
            "FROM product WHERE category_id = #{categoryId} AND status = 3")
    Map<String, Object> avgPriceByCategory(@Param("categoryId") Long categoryId);
}
