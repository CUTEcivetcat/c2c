package com.c2c.common.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.config.ConfigDataEnvironmentPostProcessor;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;

/**
 * JWT 密钥启动校验（EnvironmentPostProcessor）。
 *
 * <p>在 Spring 环境加载完成、读取到 application*.yml 与外部 config/ 配置合并结果之后执行，
 * 校验最终生效的 jwt.secret：</p>
 * <ul>
 *   <li>缺失 → 拒绝启动（提示用 openssl rand -base64 32 生成并注入 JWT_SECRET）</li>
 *   <li>等于公开已知的弱密钥/占位符 → 拒绝启动</li>
 *   <li>长度 &lt; 32 字节（HS256 要求） → 拒绝启动</li>
 * </ul>
 *
 * <p>与在 main() 里手写校验的区别：本类能读到 yml 默认值与外部 config/ 配置的合并结果，
 * 本地开发（config/application-pro.yml 有本地密钥）无需设环境变量即可启动，
 * 生产（application-prod.yml 默认空）则必须注入 JWT_SECRET，防止弱密钥上线。</p>
 */
public class JwtSecretCheckEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

    /** 禁止使用的弱 JWT 密钥（占位符 / 示例值 / 曾公开的值） */
    private static final String[] FORBIDDEN_SECRETS = {
            "change-me-to-a-random-secret-at-least-32-bytes",
            "c2c-prod-secret-key-2026-min-length-32-ok",
            "c2c-dev-secret-key-2026-min-length-32"
    };

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        String secret = environment.getProperty("jwt.secret");
        if (secret == null || secret.trim().isEmpty()) {
            throw new IllegalStateException(
                    "[安全] 未配置 JWT 密钥，拒绝启动。请通过环境变量 JWT_SECRET 注入随机密钥（openssl rand -base64 32），"
                            + "或在 config/application-*.yml 中配置 jwt.secret。");
        }
        for (String forbidden : FORBIDDEN_SECRETS) {
            if (secret.equals(forbidden)) {
                throw new IllegalStateException(
                        "[安全] JWT 密钥为公开已知的弱密钥/占位符，拒绝启动。请更换为随机密钥（openssl rand -base64 32）。");
            }
        }
        if (secret.getBytes().length < 32) {
            throw new IllegalStateException(
                    "[安全] JWT 密钥长度不足 32 字节（HS256 要求），拒绝启动。请使用 openssl rand -base64 32 生成。");
        }
    }

    @Override
    public int getOrder() {
        // 必须在 ConfigDataEnvironmentPostProcessor（加载 application*.yml 与外部 config/ 配置）之后执行：
        // order 数值更大 = 更晚执行，这样才能读到合并后的最终 jwt.secret。
        return ConfigDataEnvironmentPostProcessor.ORDER + 1;
    }
}
