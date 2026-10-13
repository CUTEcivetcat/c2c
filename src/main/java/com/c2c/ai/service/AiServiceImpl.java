package com.c2c.ai.service;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.c2c.ai.client.AiOpenAiClient;
import com.c2c.ai.config.AiProperties;
import com.c2c.ai.dto.AiPublishRequest;
import com.c2c.ai.dto.AiPublishVO;
import com.c2c.ai.mapper.ProductPriceMapper;
import com.c2c.product.entity.Category;
import com.c2c.product.mapper.CategoryMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * AI 智能发布核心实现。
 *
 * <p>流程：① Redis 缓存命中直接返回（按图去重 24h）；
 * ② 已配置大模型 Key → 多模态识别 → JSON 解析 → 分类匹配；
 * ③ 未配置 / 调用失败 / 解析失败 → 本地规则引擎兜底；
 * ④ 两种路径共用「同分类已售商品历史成交均价」修正建议价区间。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiServiceImpl implements AiService {

    /** 建议价区间：历史均价 ±20% */
    private static final BigDecimal HISTORY_LOW_RATIO = new BigDecimal("0.8");
    private static final BigDecimal HISTORY_HIGH_RATIO = new BigDecimal("1.2");

    private static final String CACHE_KEY_PREFIX = "ai:publish:";

    private final AiProperties aiProperties;
    private final AiOpenAiClient aiOpenAiClient;
    private final ProductPriceMapper productPriceMapper;
    private final CategoryMapper categoryMapper;
    private final StringRedisTemplate redisTemplate;

    @Override
    public AiPublishVO intelligentPublish(AiPublishRequest request) {
        String imageUrl = StrUtil.trim(request.getImageUrl());
        if (StrUtil.isBlank(imageUrl)) {
            throw new com.c2c.common.exception.BusinessException("请先上传商品图片");
        }

        // ① 缓存命中（按图去重，减少大模型调用成本）；Redis 不可用时视为未命中，保证功能始终可用
        String cacheKey = cacheKey(imageUrl);
        try {
            String cached = redisTemplate.opsForValue().get(cacheKey);
            if (StrUtil.isNotBlank(cached)) {
                try {
                    return JSONUtil.toBean(cached, AiPublishVO.class);
                } catch (Exception e) {
                    log.warn("AI 缓存解析失败，忽略缓存: {}", e.getMessage());
                }
            }
        } catch (Exception e) {
            log.warn("AI 缓存读取失败（忽略，继续走正常流程）: {}", e.getMessage());
        }

        AiPublishVO vo = new AiPublishVO();
        // ② 优先大模型
        boolean modelOk = false;
        if (StrUtil.isNotBlank(aiProperties.getApiKey())) {
            String content = aiOpenAiClient.chatWithImage(imageUrl);
            if (StrUtil.isNotBlank(content)) {
                modelOk = parseModelResult(content, vo);
            }
        }

        // ③ 规则引擎兜底（无 Key / 调用失败 / 解析失败）
        if (!modelOk) {
            vo.setTitle("");
            vo.setDescription("");
            vo.setCondition(2);
            // 用户已选分类时回填分类名
            if (request.getCategoryId() != null) {
                Category c = categoryMapper.selectById(request.getCategoryId());
                if (c != null) {
                    vo.setCategoryId(c.getId());
                    vo.setCategoryName(c.getName());
                }
            }
            vo.setSource("rule");
        } else {
            // 模型结果分类匹配（匹配不到 categoryId=null，前端提示手选）
            if (StrUtil.isNotBlank(vo.getCategoryName())) {
                Category matched = matchCategory(vo.getCategoryName());
                if (matched != null) {
                    vo.setCategoryId(matched.getId());
                    vo.setCategoryName(matched.getName());
                } else {
                    vo.setCategoryId(null);
                }
            }
            vo.setSource("model");
        }

        // ④ 历史成交价修正（两种路径共用；修正用最终确定的分类）
        Long finalCategoryId = vo.getCategoryId() != null ? vo.getCategoryId() : request.getCategoryId();
        if (finalCategoryId != null) {
            BigDecimal[] range = historyPriceRange(finalCategoryId);
            if (range != null) {
                vo.setPriceMin(range[0]);
                vo.setPriceMax(range[1]);
            }
        }

        // 写缓存
        try {
            redisTemplate.opsForValue().set(cacheKey, JSONUtil.toJsonStr(vo), 24, TimeUnit.HOURS);
        } catch (Exception e) {
            log.warn("AI 结果写缓存失败: {}", e.getMessage());
        }
        return vo;
    }

    /** 解析大模型 JSON，成功返回 true；任何异常返回 false 触发规则引擎兜底 */
    private boolean parseModelResult(String content, AiPublishVO vo) {
        try {
            String trimmed = StrUtil.trim(content);
            JSONObject json = JSONUtil.parseObj(trimmed);
            vo.setTitle(json.getStr("title", ""));
            vo.setDescription(json.getStr("description", ""));
            vo.setCategoryName(json.getStr("categoryName", ""));
            Object cond = json.get("condition");
            if (cond instanceof Number) {
                int c = ((Number) cond).intValue();
                vo.setCondition(c >= 1 && c <= 4 ? c : 2);
            } else {
                vo.setCondition(2);
            }
            Object min = json.get("priceMin");
            Object max = json.get("priceMax");
            vo.setPriceMin(min instanceof Number ? new BigDecimal(String.valueOf(min)) : null);
            vo.setPriceMax(max instanceof Number ? new BigDecimal(String.valueOf(max)) : null);
            return true;
        } catch (Exception e) {
            log.warn("AI 模型 JSON 解析失败，降级规则引擎: {}", e.getMessage());
            return false;
        }
    }

    /** 同分类已售商品历史成交均价 → [均价*0.8, 均价*1.2]；无历史数据返回 null */
    private BigDecimal[] historyPriceRange(Long categoryId) {
        try {
            Map<String, Object> stat = productPriceMapper.avgPriceByCategory(categoryId);
            if (stat == null || stat.get("avgPrice") == null) {
                return null;
            }
            BigDecimal avg = new BigDecimal(String.valueOf(stat.get("avgPrice")));
            if (avg.compareTo(BigDecimal.ZERO) <= 0) {
                return null;
            }
            return new BigDecimal[]{
                    avg.multiply(HISTORY_LOW_RATIO).setScale(2, RoundingMode.HALF_UP),
                    avg.multiply(HISTORY_HIGH_RATIO).setScale(2, RoundingMode.HALF_UP)
            };
        } catch (Exception e) {
            log.warn("历史成交价查询失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 分类匹配：全量分类中按名称「精确 > 包含」匹配，二级分类优先
     * （前端发布页下拉用的是二级分类 ID，故优先返回 level=2）。
     */
    private Category matchCategory(String categoryName) {
        if (StrUtil.isBlank(categoryName)) {
            return null;
        }
        List<Category> all = categoryMapper.selectList(null);
        String target = categoryName.trim();
        Category best = null;
        int bestScore = -1;
        for (Category c : all) {
            String n = c.getName() == null ? "" : c.getName().trim();
            if (n.isEmpty()) {
                continue;
            }
            int score;
            if (n.equalsIgnoreCase(target)) {
                score = 3;
            } else if (n.contains(target) || target.contains(n)) {
                score = 2;
            } else {
                continue;
            }
            if (c.getLevel() != null && c.getLevel() == 2) {
                score += 1;
            }
            if (score > bestScore) {
                bestScore = score;
                best = c;
            }
        }
        return best;
    }

    private String cacheKey(String imageUrl) {
        return CACHE_KEY_PREFIX + DigestUtil.md5Hex(imageUrl);
    }
}
