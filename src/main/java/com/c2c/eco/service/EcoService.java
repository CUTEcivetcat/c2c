package com.c2c.eco.service;

import com.c2c.eco.dto.EcoTrendVO;
import com.c2c.eco.dto.PlatformEcoVO;
import com.c2c.eco.dto.UserEcoStatsVO;

/**
 * 低碳循环统计：用户环保贡献 / 平台总览 / 减排趋势。
 */
public interface EcoService {

    /** 我的环保贡献（当前用户作为买家或卖家的已完成订单） */
    UserEcoStatsVO userStats(Long userId);

    /** 平台低碳总览（管理端数据大屏） */
    PlatformEcoVO platformOverview();

    /** 减排趋势（按已完成订单完成日期分组），days 默认 30、上限 90 */
    EcoTrendVO trend(int days);
}
