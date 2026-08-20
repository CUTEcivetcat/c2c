package com.c2c.common.filter;

import cn.hutool.core.util.StrUtil;
import com.c2c.common.utils.JwtUtils;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuthTokenFilter extends OncePerRequestFilter {

    private final StringRedisTemplate redisTemplate;

    @Value("${jwt.secret}")
    private String jwtSecret;

    /**
     * 完全公开接口（无需任何 token）。
     * 注意：/admin 已不在白名单 —— 管理接口必须携带带 role=ADMIN 的 token（见 isAdminPath）。
     */
    private static final List<String> WHITE_LIST = Arrays.asList(
            "/user/login",
            "/user/register",
            "/user/sms/send",
            "/user/reset-password",
            "/product/category",
            "/product/list",
            "/upload",
            "/admin/login",
            "/actuator",
            // Swagger 在线接口文档（免登录访问）
            "/v3/api-docs",
            "/swagger-ui",
            "/swagger-resources"
    );

    /** 游客可访问的只读接口（仅 GET 放行，避免写操作被误放行） */
    private static final List<String> READ_ONLY_GET = Arrays.asList(
            "/product/list",        // 商品列表（首页/搜索）
            "/product/category",    // 商品分类
            "/product/user",        // 查看某用户发布的在售商品（用户主页）
            "/product/comment",     // 商品评论（游客可看，发表需登录）
            "/user/profile/",       // 查看他人公开信息
            "/announcement/list",   // 平台公告列表（游客可看）
            "/announcement/latest", // 首页最新公告横幅（游客可看）
            "/announcement/force"   // 强制弹窗公告（登录后前端调用）
    );

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String path = getPathInApplication(request);
        if (isWhiteListed(request.getMethod(), path)) {
            chain.doFilter(request, response);
            return;
        }

        // 管理员接口：必须携带 role=ADMIN 的 token
        if (isAdminPath(path)) {
            String token = extractToken(request);
            if (StrUtil.isBlank(token)) {
                unauthorized(response, "请先登录管理员账号");
                return;
            }
            try {
                if (JwtUtils.isExpired(token, jwtSecret)) {
                    unauthorized(response, "管理员登录已过期");
                    return;
                }
                if (Boolean.TRUE.equals(redisTemplate.hasKey("logout:token:" + token))) {
                    unauthorized(response, "登录状态无效");
                    return;
                }
                Claims claims = JwtUtils.parseToken(token, jwtSecret);
                if (!"ADMIN".equals(claims.get("role", String.class))) {
                    unauthorized(response, "无管理员权限");
                    return;
                }
                Long userId = Long.valueOf(claims.getSubject());
                chain.doFilter(new UserHeaderRequestWrapper(request, userId), response);
                return;
            } catch (Exception e) {
                log.warn("Admin token validation failed: {}", e.getMessage());
                unauthorized(response, "管理员登录凭证无效");
                return;
            }
        }

        // 审核员接口（/review/**）：必须携带 role=2（审核员）或 ADMIN 的 token
        if (isReviewerPath(path)) {
            String token = extractToken(request);
            if (StrUtil.isBlank(token)) {
                unauthorized(response, "请先登录审核账号");
                return;
            }
            try {
                if (JwtUtils.isExpired(token, jwtSecret)) {
                    unauthorized(response, "审核登录已过期");
                    return;
                }
                if (Boolean.TRUE.equals(redisTemplate.hasKey("logout:token:" + token))) {
                    unauthorized(response, "登录状态无效");
                    return;
                }
                Claims claims = JwtUtils.parseToken(token, jwtSecret);
                String role = claims.get("role", String.class);
                if (!"ADMIN".equals(role) && !"2".equals(role)) {
                    unauthorized(response, "无审核权限");
                    return;
                }
                Long userId = Long.valueOf(claims.getSubject());
                chain.doFilter(new UserHeaderRequestWrapper(request, userId, role), response);
                return;
            } catch (Exception e) {
                log.warn("Reviewer token validation failed: {}", e.getMessage());
                unauthorized(response, "审核登录凭证无效");
                return;
            }
        }

        // 普通用户接口
        String token = extractToken(request);
        if (StrUtil.isBlank(token)) {
            unauthorized(response, "请先登录");
            return;
        }

        try {
            if (JwtUtils.isExpired(token, jwtSecret)) {
                unauthorized(response, "登录已过期");
                return;
            }
            if (Boolean.TRUE.equals(redisTemplate.hasKey("logout:token:" + token))) {
                unauthorized(response, "登录状态无效");
                return;
            }
            Long userId = JwtUtils.getUserId(token, jwtSecret);
            chain.doFilter(new UserHeaderRequestWrapper(request, userId), response);
        } catch (Exception e) {
            log.warn("Token validation failed: {}", e.getMessage());
            unauthorized(response, "登录凭证无效");
        }
    }

    private String getPathInApplication(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String contextPath = request.getContextPath();
        if (StrUtil.isNotBlank(contextPath) && uri.startsWith(contextPath)) {
            return uri.substring(contextPath.length());
        }
        return uri;
    }

    /**
     * 管理员接口路径（含 DashboardController 的 /admin/**，
     * 以及遗留的裸管理接口 /user/admin、/product/admin、/order/admin）。
     * 这些路径必须通过带 role=ADMIN 的 token 鉴权。
     */
    private boolean isAdminPath(String path) {
        return path.equals("/admin")
                || path.startsWith("/admin/")
                || path.startsWith("/user/admin")
                || path.startsWith("/product/admin")
                || path.startsWith("/order/admin");
    }

    /**
     * 审核员接口路径（审核工作台）。审核员由管理员在管理端分配（user.role=2），
     * 通过普通登录接口获得带 role=2 标记的 token，此处放行 role=2 或 ADMIN。
     */
    private boolean isReviewerPath(String path) {
        return path.equals("/review")
                || path.startsWith("/review/");
    }

    private boolean isWhiteListed(String method, String path) {
        // 无 token 白名单（登录/注册/管理等）
        for (String prefix : WHITE_LIST) {
            if (path.startsWith(prefix)) {
                return true;
            }
        }
        // 商品详情：GET /product/{id} 游客可浏览
        if ("GET".equalsIgnoreCase(method) && path.matches("/product/\\d+")) {
            return true;
        }
        // 其他只读 GET 接口游客可浏览
        if ("GET".equalsIgnoreCase(method)) {
            for (String prefix : READ_ONLY_GET) {
                if (path.startsWith(prefix)) {
                    return true;
                }
            }
        }
        return false;
    }

    private String extractToken(HttpServletRequest request) {
        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (StrUtil.isNotBlank(authHeader) && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return null;
    }

    private void unauthorized(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"code\":401,\"message\":\"" + message + "\",\"data\":null}");
    }

    private static class UserHeaderRequestWrapper extends HttpServletRequestWrapper {
        private final Long userId;
        private final String userRole;

        UserHeaderRequestWrapper(HttpServletRequest request, Long userId) {
            this(request, userId, null);
        }

        /** role 为审核接口使用：ADMIN=管理员 / 2=审核员 */
        UserHeaderRequestWrapper(HttpServletRequest request, Long userId, String userRole) {
            super(request);
            this.userId = userId;
            this.userRole = userRole;
        }

        @Override
        public String getHeader(String name) {
            if ("X-User-Id".equalsIgnoreCase(name)) {
                return String.valueOf(userId);
            }
            if ("X-User-Role".equalsIgnoreCase(name) && userRole != null) {
                return userRole;
            }
            return super.getHeader(name);
        }

        @Override
        public Enumeration<String> getHeaders(String name) {
            if ("X-User-Id".equalsIgnoreCase(name)) {
                return Collections.enumeration(Collections.singletonList(String.valueOf(userId)));
            }
            if ("X-User-Role".equalsIgnoreCase(name) && userRole != null) {
                return Collections.enumeration(Collections.singletonList(userRole));
            }
            return super.getHeaders(name);
        }
    }
}

