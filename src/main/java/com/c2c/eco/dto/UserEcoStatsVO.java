package com.c2c.eco.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 我的环保贡献（用户端个人中心卡片）。
 *
 * <p>口径（估算）：成交笔数 = 已完成订单（买家或卖家）；省钱金额 = 买家侧
 * Σ(原价-成交价)（无原价不计）；减排量 = 每笔已完成订单按一级分类系数估算 kg CO₂e；
 * 植树等效 = 减排量 / 18（一棵树年固碳量估算），向下取整。</p>
 */
@Data
public class UserEcoStatsVO {

    /** 成交笔数（作为买家或卖家的已完成订单） */
    private long dealCount;

    /** 省钱金额（元，仅买家侧 Σ(原价-成交价)） */
    private BigDecimal saveMoney;

    /** 减排量（kg CO₂e，估算口径） */
    private BigDecimal carbonReduceKg;

    /** 相当于植树（棵，减排量/18 向下取整） */
    private long treeEquivalent;
}
