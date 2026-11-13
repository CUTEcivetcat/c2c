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

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuthTokenFilter extends OncePerRequestFilter {

    private final StringRedisTemplate redisTemplate;
    private final com.c2c.user.mapper.UserMapper userMapper;

    /** 写入 request 的当前登录用户 ID 属性名，供请求日志过滤器读取 */
    public static final String ATTR_USER_ID = "c2c.userId";

    @Value("${jwt.secret}")
    private String jwtSecret;

    /**
     * 完全公开接口（无需任何 token）——固定路径，精确匹配，避免前缀越权。
     * 注意：/admin 已不在白名单 —— 管理接口必须携带带 role=ADMIN 的 token（见 isAdminPath）。
     */
    private static final Set<String> WHITE_EXACT = new HashSet<>(Arrays.asList(
            "/user/login",
            "/user/wechat-login",
            "/user/register",
            "/user/sms/send",
            "/user/reset-password",
            "/user/refresh",
            "/product/category",
            "/product/list",
            "/user/wallet/recharge/confirm",
            "/admin/login"
    ));

    /** 公开接口前缀（确需带子路径：监控/文档/静态资源），仅这些允许前缀匹配 */
    private static final List<String> WHITE_PREFIX = Arrays.asList(
            "/actuator",
            "/v3/api-docs",
            "/swagger-ui",
            "/swagger-resources"
    );

    /** 游客可访问的只读 GET 固定路径（精确匹配） */
    private static final Set<String> READ_ONLY_GET_EXACT = new HashSet<>(Arrays.asList(
            "/product/list",        // 商品列表（首页/搜索）
            "/product/category",    // 商品分类
            "/announcement/list",   // 平台公告列表（游客可看）
            "/announcement/latest", // 首页最新公告横幅（游客可看）
            "/announcement/force",   // 强制弹窗公告（登录后前端调用）
            "/announcement/publish", // 发布页展示公告
            "/banner/list",          // 首页轮播图
            "/coupon/available",     // 可领券列表（游客可看；领取/我的券仍需登录）
            "/placeholder.svg"       // 商品占位图（游客浏览列表也需要）
    ));

    /** 游客可访问的只读 GET 前缀（静态资源） */
    private static final List<String> READ_ONLY_GET_PREFIX = Arrays.asList(
            "/files"                 // 上传的图片（游客浏览商品/轮播也需要）
    );

    /** 游客可访问的只读 GET 数字 ID 路径（正则精确到 ID，避免前缀误放行） */
    private static final List<String> READ_ONLY_GET_PATTERN = Arrays.asList(
            "/product/\\d+",          // 商品详情
            "/product/user/\\d+",     // 某用户发布的在售商品（用户主页）
            "/product/comment(/\\d+)?", // 商品评论列表/单条
            "/user/profile/\\d+"      // 查看他人公开信息
    );

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String path = getPathInApplication(request);
        // 纯公开接口（登录/注册/文档/静态资源）：无 token 裸放
        if (isPublicOpen(path)) {
            chain.doFilter(request, response);
            return;
        }
        // 游客可读的只读 GET：带有效 token 则解析出登录用户以标注本人态；无 token/凭证失效按游客放行
        if (isReadOnlyGet(request.getMethod(), path)) {
            Long uid = resolveNormalUserId(request);
            if (uid != null) {
                request.setAttribute(ATTR_USER_ID, uid);
                chain.doFilter(new UserHeaderRequestWrapper(request, uid), response);
            } else {
                chain.doFilter(request, response);
            }
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
                request.setAttribute(ATTR_USER_ID, userId);
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
                request.setAttribute(ATTR_USER_ID, userId);
                chain.doFilter(new UserHeaderRequestWrapper(request, userId, role), response);
                return;
            } catch (Exception e) {
                log.warn("Reviewer token validation failed: {}", e.getMessage());
                unauthorized(response, "审核登录凭证无效");
                return;
            }
        }

        // 普通用户接口
        Long userId = resolveNormalUserId(request);
        if (userId == null) {
            unauthorized(response, "请先登录");
            return;
        }
        request.setAttribute(ATTR_USER_ID, userId);
        chain.doFilter(new UserHeaderRequestWrapper(request, userId), response);
    }

    /**
     * 解析普通用户 token，返回用户 ID；凭证缺失/过期/登出/版本失效/刷新令牌/异常时返回 null。
     * 不写 401 响应，由调用方决定是拦截还是按游客放行。
     */
    private Long resolveNormalUserId(HttpServletRequest request) {
        String token = extractToken(request);
        if (StrUtil.isBlank(token)) {
            return null;
        }
        try {
            if (JwtUtils.isExpired(token, jwtSecret)) {
                return null;
            }
            if (Boolean.TRUE.equals(redisTemplate.hasKey("logout:token:" + token))) {
                return null;
            }
            Claims claims = JwtUtils.parseToken(token, jwtSecret);
            if (isRefreshToken(claims)) {
                return null;
            }
            Long userId = Long.valueOf(claims.getSubject());
            if (isTokenVersionStale(claims, userId)) {
                return null;
            }
            return userId;
        } catch (Exception e) {
            log.warn("Token validation failed: {}", e.getMessage());
            return null;
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

    /** 纯公开接口：无任何 token 也放行（登录/注册/文档/静态资源） */
    private boolean isPublicOpen(String path) {
        if (WHITE_EXACT.contains(path)) {
            return true;
        }
        return WHITE_PREFIX.stream().anyMatch(path::startsWith);
    }

    /** 游客可读的只读 GET 接口：带有效 token 仍会解析登录态，无 token 按游客放行 */
    private boolean isReadOnlyGet(String method, String path) {
        if (!"GET".equalsIgnoreCase(method)) {
            return false;
        }
        if (READ_ONLY_GET_EXACT.contains(path)) {
            return true;
        }
        if (READ_ONLY_GET_PREFIX.stream().anyMatch(path::startsWith)) {
            return true;
        }
        for (String pattern : READ_ONLY_GET_PATTERN) {
            if (path.matches(pattern)) {
                return true;
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
        if (path.startsWith("/product/admin")) return AdminPerm.PRODUCTS;
        if (path.startsWith("/admin/orders")) return AdminPerm.ORDERS;
        if (path.startsWith("/admin/suggestion")) return AdminPerm.REVIEW;
        if (path.startsWith("/admin/announcement")) return AdminPerm.ANNOUNCEMENT;
        if (path.startsWith("/admin/popup")) return AdminPerm.ANNOUNCEMENT;
        if (path.startsWith("/admin/banner")) return AdminPerm.BANNER;
        if (path.startsWith("/admin/auth")) return AdminPerm.SECURITY;
        if (path.startsWith("/admin/faq")) return AdminPerm.AI;
        if (path.startsWith("/admin/ai")) return AdminPerm.AI;
        if (path.startsWith("/admin/coupon")) return AdminPerm.COUPON;
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

