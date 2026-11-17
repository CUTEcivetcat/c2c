package com.c2c.common.lock;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Collections;

/**
 * Redis 分布式锁：SET NX 带过期时间，释放用 Lua 脚本仅当 value 匹配才删除，
 * 避免误删其他线程/实例持有的锁。多实例部署下替代 JVM 分段锁。
 *
 * <p>用法：在 finally 中调用 {@link #unlock}；value 由调用方生成唯一标识（如 UUID）。</p>
 *
 * @author wzh
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RedisLock {

    private final StringRedisTemplate redisTemplate;

    /** 释放锁 Lua 脚本：仅当锁 value 匹配才删除 */
    private static final DefaultRedisScript<Long> UNLOCK_SCRIPT;

    static {
        UNLOCK_SCRIPT = new DefaultRedisScript<>();
        UNLOCK_SCRIPT.setScriptText(
                "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end");
        UNLOCK_SCRIPT.setResultType(Long.class);
    }

    /**
     * 尝试加锁（单次，不自旋）。
     *
     * @param key   锁键
     * @param value 锁持有者唯一标识，释放时校验
     * @param ttl   锁自动过期时间，防持锁进程崩溃死锁
     * @return true 加锁成功；false 锁被占用
     */
    public boolean tryLock(String key, String value, Duration ttl) {
        Boolean ok = redisTemplate.opsForValue().setIfAbsent(key, value, ttl);
        return Boolean.TRUE.equals(ok);
    }

    /** 释放锁：仅当 value 匹配才删除；异常只记日志，不阻断主流程 */
    public void unlock(String key, String value) {
        try {
            redisTemplate.execute(UNLOCK_SCRIPT, Collections.singletonList(key), value);
        } catch (Exception e) {
            log.warn("释放分布式锁失败: key={}, err={}", key, e.getMessage());
        }
    }
}
