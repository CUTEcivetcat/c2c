package com.c2c;

import cn.hutool.core.util.StrUtil;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 应用启动类：聚合 admin / order / product / rating 等模块，作为单体应用统一启动入口。
 */
@MapperScan("com.c2c.**.mapper")
@SpringBootApplication
public class C2cMonolithApplication {
    /** 禁止使用的弱 JWT 密钥（占位符 / 示例值 / 曾公开的值） */
    private static final String[] FORBIDDEN_SECRETS = {
            "change-me-to-a-random-secret-at-least-32-bytes",
            "c2c-prod-secret-key-2026-min-length-32-ok",
            "c2c-dev-secret-key-2026-min-length-32"
    };

    public static void main(String[] args) {
        checkJwtSecret();
        SpringApplication.run(C2cMonolithApplication.class, args);
    }

    /**
     * 启动前置校验：JWT 密钥缺失或为弱值/占位符时直接终止启动，
     * 避免使用公开已知密钥签名 token 导致任意账号/管理员伪造。
     */
    private static void checkJwtSecret() {
        String secret = System.getProperty("jwt.secret");
        if (StrUtil.isBlank(secret)) {
            secret = System.getenv("JWT_SECRET");
        }
        if (StrUtil.isBlank(secret)) {
            throw new IllegalStateException(
                    "[安全] 未配置 JWT 密钥，拒绝启动。请通过环境变量 JWT_SECRET 注入随机密钥（openssl rand -base64 32）。");
        }
        for (String forbidden : FORBIDDEN_SECRETS) {
            if (secret.equals(forbidden)) {
                throw new IllegalStateException(
                        "[安全] JWT 密钥为公开已知的弱密钥/占位符，拒绝启动。请更换为随机密钥（openssl rand -base64 32）。");
            }
        }
        if (secret.getBytes().length < 32) {
            throw new IllegalStateException(
                    "[安全] JWT 密钥长度不足 32 字节（HS256 要求），拒绝启动。");
        }
    }
}

