package com.c2c.eco.service;

import com.c2c.eco.dto.EcoTrendVO;
import com.c2c.eco.dto.PlatformEcoVO;
import com.c2c.eco.dto.UserEcoStatsVO;
import com.c2c.eco.mapper.EcoOrderMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 低碳循环统计实现。
 *
 * <p>统计口径（平台设定，答辩时标注「估算口径」，数据来自平台真实成交）：</p>
 * <ul>
 *   <li>成交笔数：order.status=4（已完成）；用户侧按买家或卖家过滤</li>
 *   <li>省钱金额：买家侧 Σ(原价-成交价)，仅计正差，无原价不计</li>
 *   <li>减排量：每笔已完成订单按商品一级分类系数估算 kg CO₂e（教材2.5 / 数码10 / 服饰6 / 美妆生活4 / 其他5）</li>
 *   <li>植树等效：减排量 / 18（一棵树年固碳量估算），向下取整</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EcoServiceImpl implements EcoService {

    /** 一棵树年固碳量估算（kg CO₂e） */
    private static final BigDecimal TREE_CO2 = new BigDecimal("18");

    /** 减排系数（按一级分类名包含匹配，kg CO₂e/件） */
    private static final Object[][] CATEGORY_COEFFICIENTS = {
            {"教材", new BigDecimal("2.5")}, {"书", new BigDecimal("2.5")},
            {"数码", new BigDecimal("10")}, {"电脑", new BigDecimal("10")}, {"办公", new BigDecimal("10")},
            {"服饰", new BigDecimal("6")}, {"鞋", new BigDecimal("6")}, {"包", new BigDecimal("6")},
            {"运动", new BigDecimal("6")}, {"户外", new BigDecimal("6")},
            {"美妆", new BigDecimal("4")}, {"护肤", new BigDecimal("4")},
            {"生活", new BigDecimal("4")}, {"家居", new BigDecimal("4")}
    };
    /** 默认系数：分类缺失 / 未匹配（kg CO₂e/件） */
    private static final BigDecimal DEFAULT_COEFFICIENT = new BigDecimal("5");

    private final EcoOrderMapper ecoOrderMapper;

    @Override
    public UserEcoStatsVO userStats(Long userId) {
        List<Map<String, Object>> rows = ecoOrderMapper.completedOrderDetail(userId, null);
        UserEcoStatsVO vo = new UserEcoStatsVO();
        BigDecimal saveMoney = BigDecimal.ZERO;
        BigDecimal carbon = BigDecimal.ZERO;
        for (Map<String, Object> row : rows) {
            // 成交笔数：每行一笔
            // 省钱金额：仅买家侧
            if (userId != null && userId.toString().equals(String.valueOf(row.get("buyerId")))) {
                saveMoney = saveMoney.add(positiveDiff(row));
            }
            carbon = carbon.add(coefficientOf(row));
        }
        vo.setDealCount(rows.size());
        vo.setSaveMoney(saveMoney.setScale(2, RoundingMode.HALF_UP));
        vo.setCarbonReduceKg(carbon.setScale(1, RoundingMode.HALF_UP));
        vo.setTreeEquivalent(carbon.divide(TREE_CO2, 0, RoundingMode.DOWN).longValue());
        return vo;
    }

    @Override
    public PlatformEcoVO platformOverview() {
        List<Map<String, Object>> rows = ecoOrderMapper.completedOrderDetail(null, null);
        PlatformEcoVO vo = new PlatformEcoVO();
        BigDecimal saveMoney = BigDecimal.ZERO;
        BigDecimal carbon = BigDecimal.ZERO;
        for (Map<String, Object> row : rows) {
            saveMoney = saveMoney.add(positiveDiff(row));
            carbon = carbon.add(coefficientOf(row));
        }
        vo.setTotalDealCount(rows.size());
        vo.setTotalSaveMoney(saveMoney.setScale(2, RoundingMode.HALF_UP));
        vo.setTotalCarbonReduceKg(carbon.setScale(1, RoundingMode.HALF_UP));
        vo.setTotalTreeEquivalent(carbon.divide(TREE_CO2, 0, RoundingMode.DOWN).longValue());
        return vo;
    }

    @Override
    public EcoTrendVO trend(int days) {
        if (days <= 0) {
            days = 30;
        }
        if (days > 90) {
            days = 90;
        }
        LocalDate today = LocalDate.now();
        LocalDate begin = today.minusDays(days - 1L);
        LocalDateTime since = LocalDateTime.of(begin, LocalTime.MIN);

        List<Map<String, Object>> rows = ecoOrderMapper.completedOrderDetail(null, since);

        // 初始化日期桶（含今天，共 days 天，升序）
        Map<String, Double> dayCarbon = new LinkedHashMap<>();
        for (int i = 0; i < days; i++) {
            dayCarbon.put(begin.plusDays(i).toString(), 0.0);
        }
        for (Map<String, Object> row : rows) {
            Object ct = row.get("completeTime");
            if (ct == null) {
                continue;
            }
            String d = String.valueOf(ct);
            if (d.length() >= 10) {
                d = d.substring(0, 10);
            }
            if (dayCarbon.containsKey(d)) {
                BigDecimal c = coefficientOf(row);
                dayCarbon.put(d, dayCarbon.get(d) + c.doubleValue());
            }
        }

        EcoTrendVO vo = new EcoTrendVO();
        List<String> dates = new ArrayList<>();
        List<Double> values = new ArrayList<>();
        for (Map.Entry<String, Double> e : dayCarbon.entrySet()) {
            dates.add(e.getKey());
            values.add(Math.round(e.getValue() * 10.0) / 10.0);
        }
        vo.setDates(dates);
        vo.setCarbonReduceKg(values);
        return vo;
    }

    /** 正差：原价-成交价，无原价或非正差返回 0 */
    private BigDecimal positiveDiff(Map<String, Object> row) {
        Object original = row.get("originalPrice");
        Object price = row.get("price");
        if (original == null || price == null) {
            return BigDecimal.ZERO;
        }
        try {
            BigDecimal diff = new BigDecimal(String.valueOf(original)).subtract(new BigDecimal(String.valueOf(price)));
            return diff.compareTo(BigDecimal.ZERO) > 0 ? diff : BigDecimal.ZERO;
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }

    /** 一级分类名 → 减排系数；分类缺失 / 未匹配返回默认 5 */
    private BigDecimal coefficientOf(Map<String, Object> row) {
        Object name = row.get("categoryName");
        if (name == null) {
            return DEFAULT_COEFFICIENT;
        }
        String categoryName = String.valueOf(name);
        for (Object[] entry : CATEGORY_COEFFICIENTS) {
            if (categoryName.contains((String) entry[0])) {
                return (BigDecimal) entry[1];
            }
        }
        return DEFAULT_COEFFICIENT;
    }
}
