package com.c2c.eco.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.c2c.order.entity.Order;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 低碳统计数据源：只读聚合 order + product + category（不改现有模块）。
 *
 * <p>注意：order 表只有 product_title/price 快照，没有 category_id / original_price，
 * 必须 LEFT JOIN product（被删除商品 join 不到时按「其他 5kg」系数、省钱不计入，保证统计不报错）。</p>
 */
@Mapper
public interface EcoOrderMapper extends BaseMapper<Order> {

    /**
     * 已完成订单明细（含商品分类与一级分类名、原价）。
     *
     * <p>{@code userId} 非空时按「买家或卖家」过滤（用户统计）；
     * 为空时返回全平台（管理端总览）。{@code since} 非空时按完成日期过滤（趋势）。</p>
     */
    @Select("<script>" +
            "SELECT o.id, o.buyer_id AS buyerId, o.seller_id AS sellerId, o.price, o.complete_time AS completeTime, " +
            "       p.category_id AS categoryId, p.original_price AS originalPrice, " +
            "       COALESCE(c2.name, c.name) AS categoryName " +
            "FROM `order` o " +
            "LEFT JOIN product p ON o.product_id = p.id " +
            "LEFT JOIN category c ON p.category_id = c.id " +
            "LEFT JOIN category c2 ON c.parent_id = c2.id AND c2.level = 1 " +
            "WHERE o.status = 4 " +
            "<if test='userId != null'> AND (o.buyer_id = #{userId} OR o.seller_id = #{userId}) </if>" +
            "<if test='since != null'> AND o.complete_time &gt;= #{since} </if>" +
            "</script>")
    List<Map<String, Object>> completedOrderDetail(@Param("userId") Long userId, @Param("since") LocalDateTime since);

    /** 平台累计省钱金额：买家侧 Σ(原价-成交价)，仅计正差 */
    @Select("SELECT COALESCE(SUM(p.original_price - o.price), 0) AS saveMoney " +
            "FROM `order` o INNER JOIN product p ON o.product_id = p.id " +
            "WHERE o.status = 4 AND p.original_price IS NOT NULL AND p.original_price > o.price")
    java.math.BigDecimal totalSaveMoney();
}
