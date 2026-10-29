package com.c2c.system.controller;

import com.c2c.common.constant.ApiPath;
import com.c2c.common.result.R;
import com.c2c.review.entity.AdminLog;
import com.c2c.review.mapper.AdminLogMapper;
import com.c2c.system.entity.SysConfig;
import com.c2c.system.service.SysConfigService;
import com.c2c.user.entity.User;
import com.c2c.user.mapper.UserMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 系统运行配置（管理端「系统设置」面板）。
 *
 * <p>仅总管理员（admin_level=2）可读写：AuthTokenFilter 对 /admin/config 无权限码映射，
 * 次级/子级管理员默认拒绝；此处再按库中 admin_level 兜底校验，防止旧 token 越权。</p>
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@Tag(name = "系统设置", description = "运行时可动态调整的系统开关（验证码回显、管理员登录锁定等），仅总管理员")
public class AdminConfigController {

    private final SysConfigService sysConfigService;
    private final UserMapper userMapper;
    private final AdminLogMapper adminLogMapper;

    @Operation(summary = "配置列表（总管理员）", description = "返回全部系统运行配置及说明")
    @GetMapping(ApiPath.ADMIN_CONFIG)
    public R<List<SysConfig>> list(@Parameter(hidden = true) @RequestHeader("X-User-Id") Long userId) {
        requireTopAdmin(userId);
        return R.ok(sysConfigService.listAll());
    }

    @Operation(summary = "更新配置（总管理员）", description = "body: {key, value}；写库并刷新缓存，同时记录操作日志")
    @PutMapping(ApiPath.ADMIN_CONFIG)
    public R<Void> update(@Parameter(hidden = true) @RequestHeader("X-User-Id") Long userId,
                          @RequestBody Map<String, String> body) {
        requireTopAdmin(userId);
        String key = body.get("key");
        String value = body.get("value");
        if (key == null || key.trim().isEmpty() || value == null || value.trim().isEmpty()) {
            throw new com.c2c.common.exception.BusinessException("配置键与值不能为空");
        }
        sysConfigService.update(key.trim(), value.trim(), userId);

        AdminLog adminLog = new AdminLog();
        adminLog.setOperatorId(userId);
        adminLog.setOperatorRole(1);
        adminLog.setAction("CONFIG_UPDATE");
        adminLog.setTargetType("sys_config");
        adminLog.setTargetId(null);
        adminLog.setDetail("更新系统配置 " + key.trim() + " = " + value.trim());
        adminLogMapper.insert(adminLog);
        return R.ok();
    }

    /** 兜底校验：仅总管理员（admin_level=2）可操作 */
    private void requireTopAdmin(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null || user.getAdminLevel() == null || user.getAdminLevel() < 2) {
            log.warn("非总管理员尝试访问系统设置：userId={}", userId);
            throw new com.c2c.common.exception.BusinessException(403, "仅总管理员可管理系统设置");
        }
    }
}
