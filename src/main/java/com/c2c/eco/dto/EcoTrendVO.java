package com.c2c.eco.dto;

import lombok.Data;

import java.util.List;

/**
 * 减排趋势（管理端数据大屏折线图）：按已完成订单的完成日期分组。
 */
@Data
public class EcoTrendVO {

    /** 日期列表（yyyy-MM-dd，升序） */
    private List<String> dates;

    /** 每日减排量（kg CO₂e，估算口径），与 dates 一一对应 */
    private List<Double> carbonReduceKg;
}
