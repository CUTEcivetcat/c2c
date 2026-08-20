package com.c2c.admin.controller;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.c2c.common.constant.ApiPath;
import com.c2c.common.exception.BusinessException;
import com.c2c.common.result.R;
import com.c2c.common.utils.JwtUtils;
import com.c2c.user.entity.User;
import com.c2c.user.mapper.UserMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

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
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

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

        // 仅允许 role=1 的管理员账号登录，按手机号/邮箱/用户名匹配
        User admin = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getRole, 1)
                .and(w -> w.eq(User::getPhone, account)
                        .or().eq(User::getEmail, account)
                        .or().eq(User::getUsername, account)));

        if (admin == null) {
            log.warn("管理员登录失败：账号不存在，account={}", account);
            throw new BusinessException(401, "用户名或密码错误");
        }
        if (admin.getStatus() != null && admin.getStatus() == 0) {
            log.warn("管理员登录失败：账号已禁用，userId={}", admin.getId());
            throw new BusinessException(401, "该管理员账号已被禁用");
        }
        if (!passwordEncoder.matches(password, admin.getPassword())) {
            log.warn("管理员登录失败：密码错误，account={}", account);
            throw new BusinessException(401, "用户名或密码错误");
        }

        // 签发带角色标记的 token，供 AuthTokenFilter 校验
        Map<String, Object> claims = new HashMap<>();
        claims.put("role", "ADMIN");
        String token = JwtUtils.createToken(admin.getId(), claims, jwtSecret, jwtExpiration);

        Map<String, Object> data = new HashMap<>();
        data.put("token", token);
        data.put("expiresIn", jwtExpiration);
        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("id", admin.getId());
        userInfo.put("username", admin.getUsername());
        userInfo.put("nickname", admin.getNickname());
        data.put("userInfo", userInfo);

        log.info("管理员登录成功：userId={}, account={}", admin.getId(), account);
        return R.ok(data);
    }
}
