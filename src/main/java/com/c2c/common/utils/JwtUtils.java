package com.c2c.common.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;

/**
 * JWT 工具类：Token 的生成、解析、过期与有效性校验。
 */
public class JwtUtils {

    /**
     * 生成 Token
     */
    public static String createToken(Long userId, String secret, long expiration) {
        SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        Date now = new Date();
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expiration * 1000))
                .signWith(key)
                .compact();
    }

    /**
     * 生成 Token（带自定义Claims）
     */
    public static String createToken(Long userId, Map<String, Object> claims, String secret, long expiration) {
        SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        Date now = new Date();
        return Jwts.builder()
                .claims(claims)
                .subject(String.valueOf(userId))
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expiration * 1000))
                .signWith(key)
                .compact();
    }

    /**
     * 解析 Token
     */
    public static Claims parseToken(String token, String secret) {
        SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * 从 Token 中获取用户 ID
     */
    public static Long getUserId(String token, String secret) {
        Claims claims = parseToken(token, secret);
        return Long.valueOf(claims.getSubject());
    }

    /**
     * 判断 Token 是否过期
     */
    public static boolean isExpired(String token, String secret) {
        try {
            Claims claims = parseToken(token, secret);
            return claims.getExpiration().before(new Date());
        } catch (Exception e) {
            return true;
        }
    }

    /**
     * 验证 Token 是否有效
     */
    public static boolean validate(String token, String secret) {
        try {
            parseToken(token, secret);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}



