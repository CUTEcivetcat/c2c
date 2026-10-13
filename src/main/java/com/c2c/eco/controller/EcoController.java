package com.c2c.eco.controller;

import com.c2c.common.constant.ApiPath;
import com.c2c.common.result.R;
import com.c2c.eco.dto.EcoTrendVO;
import com.c2c.eco.dto.PlatformEcoVO;
import com.c2c.eco.dto.UserEcoStatsVO;
import com.c2c.eco.service.EcoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 低碳循环数据：用户环保贡献（登录）/ 平台总览与趋势（管理端，DASHBOARD 权限）。
 */
@RestController
@RequiredArgsConstructor
@Tag(name = "低碳循环看板", description = "用户环保贡献 + 平台减排总览/趋势（估算口径）")
public class EcoController {

    private final EcoService ecoService;

    @Operation(summary = "我的环保贡献", description = "成交笔数 / 累计省钱 / 减排量(kg CO₂e) / 相当于植树（需登录）")
    @GetMapping(ApiPath.ECO_USER_STATS)
    public R<UserEcoStatsVO> userStats(@Parameter(hidden = true) @RequestHeader("X-User-Id") Long userId) {
        return R.ok(ecoService.userStats(userId));
    }

    @Operation(summary = "平台低碳总览", description = "累计成交 / 累计省钱 / 累计减排量 / 植树等效（管理端，数据大屏权限）")
    @GetMapping(ApiPath.ADMIN_ECO_OVERVIEW)
    public R<PlatformEcoVO> overview() {
        return R.ok(ecoService.platformOverview());
    }

    @Operation(summary = "减排趋势", description = "近 N 日减排量折线（管理端，数据大屏权限；days 默认 30、上限 90）")
    @GetMapping(ApiPath.ADMIN_ECO_TREND)
    public R<EcoTrendVO> trend(@Parameter(description = "天数") @RequestParam(defaultValue = "30") int days) {
        return R.ok(ecoService.trend(days));
    }
}
