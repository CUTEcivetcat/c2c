package com.c2c;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 应用启动类：聚合 admin / order / product / rating 等模块，作为单体应用统一启动入口。
 *
 * <p>JWT 密钥启动校验由 {@code JwtSecretCheckEnvironmentPostProcessor} 负责
 * （见 spring.factories 注册），会在 Spring 环境加载完成后校验，避免弱密钥上线。</p>
 */
@MapperScan("com.c2c.**.mapper")
@SpringBootApplication
public class C2cMonolithApplication {
    public static void main(String[] args) {
        SpringApplication.run(C2cMonolithApplication.class, args);
    }
}
