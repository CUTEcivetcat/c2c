package com.c2c.common.filter;

import cn.hutool.core.util.StrUtil;
import com.c2c.common.constant.AdminPerm;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuthTokenFilter extends OncePerRequestFilter {

    private final StringRedisTemplate redisTemplate;
    private final com.c2c.user.mapper.UserMapper userMapper;

    @Value("${jwt.secret}")
    private String jwtSecret;

    /**
     * 完全公开接口（无需任何 token）。
     * 注意：/admin 已不在白名单 —— 管理接口必须携带带 role=ADMIN 的 token（见 isAdminPath）。
     */
    private static final List<String> WHITE_LIST = Arrays.asList(
            "/user/login",
            "/user/wechat-login",
            "/user/register",
            "/user/sms/send",
            "/user/reset-password",
            "/user/refresh",
            "/product/category",
            "/product/list",
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
            "/announcement/force",   // 强制弹窗公告（登录后前端调用）
            "/announcement/publish", // 发布页展示公告
            "/banner/list"           // 首页轮播图
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
                // 刷新令牌禁止访问业务接口；令牌版本比对（改密/封禁/权限变更后旧令牌立即失效）
                if (isRefreshToken(claims)) {
                    unauthorized(response, "刷新令牌不能访问业务接口");
                    return;
                }
                Long userId = Long.valueOf(claims.getSubject());
                if (isTokenVersionStale(claims, userId)) {
                    unauthorized(response, "登录状态已失效，请重新登录");
                    return;
                }
                // 三级权限校验：admin_level=2 总管理员全放行；次级/子级管理员按 token 内的 adminPerms 校验模块权限
                Integer adminLevel = claims.get("adminLevel", Integer.class);
                if (adminLevel == null || adminLevel < 2) {
                    if (!hasAdminPerm(claims, path)) {
                        unauthorized(response, "无该模块的操作权限");
                        return;
                    }
                }
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
                if (isRefreshToken(claims)) {
                    unauthorized(response, "刷新令牌不能访问业务接口");
                    return;
                }
                Long userId = Long.valueOf(claims.getSubject());
                if (isTokenVersionStale(claims, userId)) {
                    unauthorized(response, "登录状态已失效，请重新登录");
                    return;
                }
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
            Claims claims = JwtUtils.parseToken(token, jwtSecret);
            if (isRefreshToken(claims)) {
                unauthorized(response, "刷新令牌不能访问业务接口");
                return;
            }
            Long userId = Long.valueOf(claims.getSubject());
            if (isTokenVersionStale(claims, userId)) {
                unauthorized(response, "登录状态已失效，请重新登录");
                return;
            }
            chain.doFilter(new UserHeaderRequestWrapper(request, userId), response);
        } catch (Exception e) {
            log.warn("Token validation failed: {}", e.getMessage());
            unauthorized(response, "登录凭证无效");
        }
    }

    /** 刷新令牌（type=refresh）只允许调用 /user/refresh，禁止访问一切业务接口 */
    private boolean isRefreshToken(Claims claims) {
        return "refresh".equals(claims.get("type", String.class));
    }

    /**
     * 令牌版本比对：token 内的 tv 声明与当前用户版本不一致（改密/封禁/角色权限变更后递增过）即失效。
     * 老 token 无 tv 声明按 0 处理，在发生第一次失效事件前保持兼容。
     */
    private boolean isTokenVersionStale(Claims claims, Long userId) {
        Integer tokenTv = claims.get("tv", Integer.class);
        int tv = tokenTv == null ? 0 : tokenTv;
        return tv != currentTokenVersion(userId);
    }

    /** 当前令牌版本：Redis 缓存优先，缺失时回源数据库并回填 */
    private int currentTokenVersion(Long userId) {
        String cached = redisTemplate.opsForValue().get("user:tv:" + userId);
        if (cached != null) {
            try {
                return Integer.parseInt(cached);
            } catch (NumberFormatException ignored) {
                // 缓存异常时回源数据库
            }
        }
        com.c2c.user.entity.User user = userMapper.selectById(userId);
        if (user == null) {
            return Integer.MIN_VALUE; // 用户不存在：与任何 tv 都不相等 → 拒绝
        }
        int tv = user.getTokenVersion() == null ? 0 : user.getTokenVersion();
        redisTemplate.opsForValue().set("user:tv:" + userId, String.valueOf(tv));
        return tv;
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

    /**
     * 次级/子级管理员的模块权限校验（总管理员 admin_level=2 不走此方法）。
     *
     * <ul>
     *   <li>设置自己的二级密码（/admin/auth/secondary-password）：管理员基础能力，无需模块权限</li>
     *   <li>次级管理员管理（/admin/auth/secondary-admins*）：仅总管理员</li>
     *   <li>角色分配（/admin/users/roles、/admin/users/{id}/role）：仅总管理员</li>
     *   <li>其余 /admin/** 按路径映射到权限码，检查 token 内 adminPerms</li>
     * </ul>
     */
    private boolean hasAdminPerm(Claims claims, String path) {
        if (path.equals("/admin/auth/secondary-password")) {
            return true;
        }
        if (path.startsWith("/admin/auth/secondary-admins")) {
            return false;
        }
        if (path.startsWith("/admin/users/roles") || path.matches("/admin/users/\\d+/role")) {
            return false;
        }
        String need = permForPath(path);
        if (need == null) {
            return false;
        }
        Object permsObj = claims.get("perms");
        if (permsObj == null) {
            return false;
        }
        List<String> perms = new ArrayList<>();
        if (permsObj instanceof List) {
            for (Object o : (List<?>) permsObj) {
                perms.add(String.valueOf(o));
            }
        } else {
            perms.addAll(Arrays.asList(String.valueOf(permsObj).split(",")));
        }
        return perms.contains(need) || perms.contains("*");
    }

    /** /admin/** 路径 → 模块权限码。 */
    private String permForPath(String path) {
        if (path.startsWith("/admin/dashboard")) return AdminPerm.DASHBOARD;
        if (path.startsWith("/admin/eco")) return AdminPerm.DASHBOARD;
        if (path.startsWith("/admin/users")) return AdminPerm.USERS;
        if (path.startsWith("/admin/products")) return AdminPerm.PRODUCTS;
        if (path.startsWith("/admin/orders")) return AdminPerm.ORDERS;
        if (path.startsWith("/admin/announcement")) return AdminPerm.ANNOUNCEMENT;
        if (path.startsWith("/admin/banner")) return AdminPerm.BANNER;
        if (path.startsWith("/admin/auth")) return AdminPerm.SECURITY;
        return null;
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

