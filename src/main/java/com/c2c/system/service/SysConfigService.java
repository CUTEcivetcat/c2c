package com.c2c.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.c2c.system.entity.SysConfig;
import com.c2c.system.mapper.SysConfigMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 系统运行配置服务。
 *
 * <p>读取：Redis 读穿缓存（key={@code sys:config:{configKey}}），未命中查库并回填；
 * 未配置时返回调用方传入的默认值（通常来自配置文件，保证未初始化 sys_config 表时系统可用）。
 * 更新：写库并同步刷新缓存，管理端「系统设置」面板调用。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SysConfigService {

    private static final String CACHE_PREFIX = "sys:config:";

    private final SysConfigMapper sysConfigMapper;
    private final StringRedisTemplate redisTemplate;

    /** 读取字符串配置，未配置时返回默认值 */
    public String get(String key, String defaultValue) {
        String cached = redisTemplate.opsForValue().get(CACHE_PREFIX + key);
        if (cached != null) {
            return cached;
        }
        try {
            SysConfig config = sysConfigMapper.selectOne(
                    new LambdaQueryWrapper<SysConfig>().eq(SysConfig::getConfigKey, key));
            if (config == null) {
                return defaultValue;
            }
            redisTemplate.opsForValue().set(CACHE_PREFIX + key, config.getConfigValue());
            return config.getConfigValue();
        } catch (Exception e) {
            // 兼容尚未执行 sys_config 建表脚本的老库：降级为默认值，保证登录等核心链路不受影响
            log.warn("sys_config 读取失败（老库未建表？），key={} 使用默认值 {}：{}", key, defaultValue, e.getMessage());
            return defaultValue;
        }
    }

    /** 读取布尔配置（值为 "true"/"false"，大小写不敏感），未配置时返回默认值 */
    public boolean getBool(String key, boolean defaultValue) {
        String value = get(key, null);
        if (value == null) {
            return defaultValue;
        }
        return Boolean.parseBoolean(value.trim());
    }

    /** 读取整数配置，未配置或格式非法时返回默认值 */
    public int getInt(String key, int defaultValue) {
        String value = get(key, null);
        if (value == null) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            log.warn("sys_config 值非法，key={}, value={}, 使用默认值 {}", key, value, defaultValue);
            return defaultValue;
        }
    }

    /** 更新配置（仅允许更新已存在的配置键），写库并刷新缓存 */
    public void update(String key, String value, Long operatorId) {
        SysConfig config = sysConfigMapper.selectOne(
                new LambdaQueryWrapper<SysConfig>().eq(SysConfig::getConfigKey, key));
        if (config == null) {
            throw new com.c2c.common.exception.BusinessException("不支持的配置项：" + key);
        }
        config.setConfigValue(value);
        config.setUpdatedBy(operatorId);
        sysConfigMapper.updateById(config);
        redisTemplate.opsForValue().set(CACHE_PREFIX + key, value);
        log.info("sys_config 更新：key={}, value={}, operatorId={}", key, value, operatorId);
    }

    /** 全部配置（管理端面板展示） */
    public List<SysConfig> listAll() {
        return sysConfigMapper.selectList(new LambdaQueryWrapper<SysConfig>().orderByAsc(SysConfig::getId));
    }
}
