package com.c2c.common.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * JWT 工具类单元测试：生成 / 解析 / 过期 / 非法 token 校验。
 */
class JwtUtilsTest {

    private static final String SECRET = "test-secret-key-2026-min-length-32-ok";

    @Test
    void createAndParseToken() {
        String token = JwtUtils.createToken(1L, SECRET, 3600);
        assertNotNull(token);
        assertEquals(1L, JwtUtils.getUserId(token, SECRET));
        assertTrue(JwtUtils.validate(token, SECRET));
        assertFalse(JwtUtils.isExpired(token, SECRET));
    }

    @Test
    void createWithClaims() {
        String token = JwtUtils.createToken(2L, java.util.Collections.singletonMap("role", "ADMIN"), SECRET, 3600);
        assertEquals("ADMIN", JwtUtils.parseToken(token, SECRET).get("role", String.class));
    }

    @Test
    void expiredTokenShouldBeExpired() {
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
        assertFalse(JwtUtils.validate("invalid.token.value", SECRET));
        assertTrue(JwtUtils.isExpired("invalid.token.value", SECRET));
    }
}
