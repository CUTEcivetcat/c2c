package com.c2c.common.ratelimit;

import com.c2c.common.filter.AuthTokenFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.time.Duration;

/**
 * 限流拦截器：对标注 {@link RateLimit} 的 Controller 方法按用户（未登录按 IP）+ 接口计数，
 * Redis INCR 窗口配额，超限返回 429。
 *
 * @author wzh
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RateLimitInterceptor implements HandlerInterceptor {

    private static final String KEY_PREFIX = "rl:";

    private final StringRedisTemplate redisTemplate;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws IOException {
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }
        RateLimit rateLimit = handlerMethod.getMethodAnnotation(RateLimit.class);
        if (rateLimit == null) {
            return true;
        }

        String identity = resolveIdentity(request);
        String methodKey = handlerMethod.getBeanType().getSimpleName() + "#" + handlerMethod.getMethod().getName();
        String key = KEY_PREFIX + methodKey + ":" + identity;

        Long count = redisTemplate.opsForValue().increment(key);
        if (count != null && count == 1) {
            redisTemplate.expire(key, Duration.ofSeconds(rateLimit.windowSec()));
        }
        if (count != null && count > rateLimit.limit()) {
            log.warn("接口限流触发: key={}, count={}, limit={}", key, count, rateLimit.limit());
            writeTooManyRequests(response, rateLimit.message());
            return false;
        }
        return true;
    }

    /** 限流标识：优先登录用户 ID，未登录回退 IP */
    private String resolveIdentity(HttpServletRequest request) {
        Object userId = request.getAttribute(AuthTokenFilter.ATTR_USER_ID);
        if (userId != null) {
            return "u:" + userId;
        }
        return "ip:" + resolveClientIp(request);
    }

    private String resolveClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }
        return request.getRemoteAddr();
    }

    private void writeTooManyRequests(HttpServletResponse response, String message) throws IOException {
        response.setStatus(429);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"code\":429,\"message\":\"" + message + "\",\"data\":null}");
    }
}
