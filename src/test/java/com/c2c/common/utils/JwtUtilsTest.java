package com.c2c.common.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * JWT 工具类单元测试：生成 / 解析 / 过期 / 非法 token 校验。
 */
class JwtUtilsTest {

    private static final String SECRET = "test-secret-key-2026-min-length-32-ok";

    @org.junit.jupiter.api.BeforeAll
    static void header() {
        com.c2c.common.infra.TestLog.log("[测试类] JWT 工具：生成 / 解析 / 过期 / 非法token校验");
    }

    @Test
    void createAndParseToken() {
        com.c2c.common.infra.TestLog.log("[测试] 生成并解析：subject=用户ID，签名校验通过，未过期");
        String token = JwtUtils.createToken(1L, SECRET, 3600);
        assertNotNull(token);
        assertEquals(1L, JwtUtils.getUserId(token, SECRET));
        assertTrue(JwtUtils.validate(token, SECRET));
        assertFalse(JwtUtils.isExpired(token, SECRET));
    }

    @Test
    void createWithClaims() {
        com.c2c.common.infra.TestLog.log("[测试] 带声明生成：role=ADMIN 写入 claims 并可读回");
        String token = JwtUtils.createToken(2L, java.util.Collections.singletonMap("role", "ADMIN"), SECRET, 3600);
        assertEquals("ADMIN", JwtUtils.parseToken(token, SECRET).get("role", String.class));
    }

    @Test
    void expiredTokenShouldBeExpired() {
        com.c2c.common.infra.TestLog.log("[测试] 过期判定：1秒有效期的令牌，1.1秒后 isExpired=true");
        String token = JwtUtils.createToken(1L, SECRET, 1); // 1 秒过期
        assertFalse(JwtUtils.isExpired(token, SECRET));
        try {
            Thread.sleep(1100);
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
        assertTrue(JwtUtils.isExpired(token, SECRET));
        assertTrue(JwtUtils.validate(token, SECRET) || JwtUtils.isExpired(token, SECRET));
    }

    @Test
    void invalidTokenShouldBeRejected() {
        com.c2c.common.infra.TestLog.log("[测试] 非法令牌：validate=false，isExpired=true（解析失败视为过期）");
        assertFalse(JwtUtils.validate("invalid.token.value", SECRET));
        assertTrue(JwtUtils.isExpired("invalid.token.value", SECRET));
    }
}
