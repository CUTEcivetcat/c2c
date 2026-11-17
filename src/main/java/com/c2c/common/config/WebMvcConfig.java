package com.c2c.common.config;

import com.c2c.common.ratelimit.RateLimitInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Paths;

/**
 * 将上传目录映射为 /files/** 静态资源，供本地开发访问上传的图片。
 * 生产环境由 Nginx 的 /files/ 直接映射 uploads 目录，本配置与之不冲突。
 * 同时注册接口限流拦截器（仅对标注 @RateLimit 的方法生效）。
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Value("${app.upload-root}")
    private String uploadRoot;

    @Value("${app.upload-url-prefix:/files}")
    private String uploadUrlPrefix;

    private final RateLimitInterceptor rateLimitInterceptor;

    public WebMvcConfig(RateLimitInterceptor rateLimitInterceptor) {
        this.rateLimitInterceptor = rateLimitInterceptor;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String location = Paths.get(uploadRoot).toUri().toString();
        registry.addResourceHandler(uploadUrlPrefix + "/**")
                .addResourceLocations(location);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(rateLimitInterceptor).addPathPatterns("/**");
    }
}
