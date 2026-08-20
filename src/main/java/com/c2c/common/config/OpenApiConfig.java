package com.c2c.common.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI / Swagger 文档配置。
 * 访问地址：http://<host>:<port>/api/v1/swagger-ui/index.html
 * 原始 JSON：http://<host>:<port>/api/v1/v3/api-docs
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI c2cOpenAPI() {
        return new OpenAPI().info(new Info()
                .title("闲小鱼 C2C 二手交易平台 API")
                .description("用户端 / 管理端全部接口。除标注「公开」外，均需携带请求头 Authorization: Bearer <token>（登录接口返回）。")
                .version("1.0.0"));
    }
}
