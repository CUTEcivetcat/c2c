package com.c2c.common.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 请求日志过滤器：记录每个 /api 请求的方法、路径、响应状态和耗时，便于排查问题。
 * 放在过滤器链最外层（AuthTokenFilter 之前）。
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestLogFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String uri = request.getRequestURI();
        // 只记录业务接口，过滤健康检查等频繁调用
        boolean shouldLog = uri.startsWith("/api") && !uri.contains("/actuator/health");

        long start = System.currentTimeMillis();
        try {
            chain.doFilter(request, response);
        } finally {
            if (shouldLog) {
                String query = request.getQueryString();
                String path = uri + (query != null ? "?" + query : "");
                long cost = System.currentTimeMillis() - start;
                Object uid = request.getAttribute(AuthTokenFilter.ATTR_USER_ID);
                String userPart = uid != null ? " userId=" + uid : "";
                log.info("[req] {} {} -> {} ({}ms){}", request.getMethod(), path, response.getStatus(), cost, userPart);
            }
        }
    }
}
