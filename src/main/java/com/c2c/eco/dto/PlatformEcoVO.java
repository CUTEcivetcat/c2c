package com.c2c.eco.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 平台低碳总览（管理端数据大屏）。
 */
@Data
public class PlatformEcoVO {

    /** 平台累计成交笔数（已完成订单） */
    private long totalDealCount;

    /** 平台累计省钱金额（元，买家侧 Σ(原价-成交价)） */
    private BigDecimal totalSaveMoney;

    /** 平台累计减排量（kg CO₂e，估算口径） */
    private BigDecimal totalCarbonReduceKg;

    /** 相当于植树（棵） */
    private long totalTreeEquivalent;
}
