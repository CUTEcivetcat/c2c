package com.c2c.admin.controller;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.c2c.common.constant.ApiPath;
import com.c2c.common.exception.BusinessException;
import com.c2c.common.result.R;
import com.c2c.common.utils.JwtUtils;
import com.c2c.system.constant.SysConfigKey;
import com.c2c.system.service.SysConfigService;
import com.c2c.user.entity.User;
import com.c2c.user.mapper.UserMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 管理员认证：管理员登录接口。
 *
 * <p>说明：管理员账号存在 user 表里，靠 role=1 标记（0=普通用户，1=管理员）。
 * 登录成功后签发带 role=ADMIN 标记的 JWT，AuthTokenFilter 对 /admin/** 等接口
 * 校验该标记，杜绝之前「管理接口无任何鉴权」的问题。
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@Tag(name = "管理员认证", description = "管理员登录（account 支持手机号 / 邮箱 / 用户名）")
public class AdminAuthController {

    private final UserMapper userMapper;
    private final SysConfigService sysConfigService;
    private final StringRedisTemplate redisTemplate;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    /** 登录失败锁定 Redis 键前缀 */
    private static final String FAIL_KEY_PREFIX = "adminlogin:fail:";
    private static final String LOCK_KEY_PREFIX = "adminlogin:lock:";

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.expiration}")
    private long jwtExpiration;

    /**
     * 管理员登录：account 支持 手机号 / 邮箱 / 用户名
     */
    @Operation(summary = "管理员登录", description = "account 支持手机号 / 邮箱 / 用户名，密码用 BCrypt 校验，成功返回带 role=ADMIN 的 token")
    @PostMapping(ApiPath.ADMIN_LOGIN)
    public R<Map<String, Object>> login(@RequestBody Map<String, String> body) {
        String account = body.get("account");
        String password = body.get("password");
        if (StrUtil.isBlank(account) || StrUtil.isBlank(password)) {
            throw new BusinessException("账号或密码不能为空");
        }

        checkLoginLock(account);

        // 仅允许 role=1 的管理员账号登录，按手机号/邮箱/用户名匹配
        User admin = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getRole, 1)
                .and(w -> w.eq(User::getPhone, account)
                        .or().eq(User::getEmail, account)
                        .or().eq(User::getUsername, account)));

        if (admin == null) {
            log.warn("管理员登录失败：账号不存在，account={}", account);
            failLoginLock(account);
            throw new BusinessException(401, "用户名或密码错误");
        }
        if (admin.getStatus() != null && admin.getStatus() == 0) {
            log.warn("管理员登录失败：账号已禁用，userId={}", admin.getId());
            throw new BusinessException(401, "该管理员账号已被禁用");
        }
        if (!passwordEncoder.matches(password, admin.getPassword())) {
            log.warn("管理员登录失败：密码错误，account={}", account);
            failLoginLock(account);
            throw new BusinessException(401, "用户名或密码错误");
        }

        clearLoginLock(account);

        // 三级权限自愈：username='admin' 恒为总管理员(admin_level=2)，
        // 老数据/漏执行迁移 SQL 导致 admin_level 缺失或为 0 时，登录即自动升级并写库，
        // 保证 admin 账号即使未执行 update-20260915-admin-levels.sql 也能获得全部权限。
        boolean levelChanged = false;
        if (admin.getAdminLevel() == null || admin.getAdminLevel() < 2) {
            if ("admin".equals(admin.getUsername())) {
                admin.setAdminLevel(2);
                admin.setAdminPerms("*");
                levelChanged = true;
            } else if (admin.getAdminLevel() == null || admin.getAdminLevel() == 0) {
                // 存量管理员（role=1 且非 admin）默认转为次级管理员，权限由总管理员后续分配
                admin.setAdminLevel(1);
                levelChanged = true;
            }
        }
        if (levelChanged) {
            userMapper.updateById(admin);
            log.info("管理员等级自愈修复：userId={}, username={}, adminLevel={}", admin.getId(), admin.getUsername(), admin.getAdminLevel());
        }

        // 签发带角色与权限标记的 token，供 AuthTokenFilter 校验
        Map<String, Object> claims = new HashMap<>();
        claims.put("role", "ADMIN");
        claims.put("adminLevel", admin.getAdminLevel() == null ? 0 : admin.getAdminLevel());
        claims.put("perms", parsePerms(admin.getAdminLevel(), admin.getAdminPerms()));
        claims.put("tv", admin.getTokenVersion() == null ? 0 : admin.getTokenVersion());
        String token = JwtUtils.createToken(admin.getId(), claims, jwtSecret, jwtExpiration);
        redisTemplate.opsForValue().set("user:tv:" + admin.getId(),
                String.valueOf(admin.getTokenVersion() == null ? 0 : admin.getTokenVersion()));

        Map<String, Object> data = new HashMap<>();
        data.put("token", token);
        data.put("expiresIn", jwtExpiration);
        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("id", admin.getId());
        userInfo.put("username", admin.getUsername());
        userInfo.put("nickname", admin.getNickname());
        userInfo.put("adminLevel", admin.getAdminLevel() == null ? 0 : admin.getAdminLevel());
        userInfo.put("adminPerms", admin.getAdminLevel() != null && admin.getAdminLevel() >= 2 ? "*" : admin.getAdminPerms());
        data.put("userInfo", userInfo);

        log.info("管理员登录成功：userId={}, account={}, adminLevel={}", admin.getId(), account, admin.getAdminLevel());
        return R.ok(data);
    }

    /** 总管理员(admin_level=2)恒为全部权限；其余管理员按 adminPerms 拆分。 */
    private java.util.List<String> parsePerms(Integer adminLevel, String adminPerms) {
        java.util.List<String> perms = new java.util.ArrayList<>();
        if (adminLevel != null && adminLevel >= 2) {
            perms.add("*");
        } else if (adminPerms != null && !adminPerms.trim().isEmpty()) {
            for (String p : adminPerms.split(",")) {
                String t = p.trim();
                if (!t.isEmpty()) {
                    perms.add(t);
                }
            }
        }
        return perms;
    }

    // ==================== 登录失败锁定（开关由 sys_config 控制） ====================

    private boolean lockEnabled() {
        return sysConfigService.getBool(SysConfigKey.ADMIN_LOGIN_LOCK_ENABLED, true);
    }

    private void checkLoginLock(String account) {
        if (!lockEnabled()) {
            return;
        }
        if (Boolean.TRUE.equals(redisTemplate.hasKey(LOCK_KEY_PREFIX + account))) {
            throw new BusinessException("登录失败次数过多，账号已锁定，请稍后再试");
        }
    }

    private void failLoginLock(String account) {
        if (!lockEnabled()) {
            return;
        }
        int maxAttempts = sysConfigService.getInt(SysConfigKey.ADMIN_LOGIN_LOCK_MAX_ATTEMPTS, 5);
        int lockMinutes = sysConfigService.getInt(SysConfigKey.ADMIN_LOGIN_LOCK_LOCK_MINUTES, 15);
        String failKey = FAIL_KEY_PREFIX + account;
        Long fails = redisTemplate.opsForValue().increment(failKey);
        if (fails != null && fails == 1L) {
            redisTemplate.expire(failKey, 60, TimeUnit.MINUTES);
        }
        if (fails != null && fails >= maxAttempts) {
            redisTemplate.opsForValue().set(LOCK_KEY_PREFIX + account, "1", lockMinutes, TimeUnit.MINUTES);
            redisTemplate.delete(failKey);
            log.warn("管理员登录连续失败 {} 次，锁定 {} 分钟：account={}", maxAttempts, lockMinutes, account);
            throw new BusinessException("登录失败" + maxAttempts + "次，账号已锁定" + lockMinutes + "分钟");
        }
    }

    private void clearLoginLock(String account) {
        redisTemplate.delete(FAIL_KEY_PREFIX + account);
    }
}
