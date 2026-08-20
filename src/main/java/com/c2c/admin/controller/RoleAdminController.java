package com.c2c.admin.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.c2c.common.constant.ApiPath;
import com.c2c.common.exception.BusinessException;
import com.c2c.common.result.R;
import com.c2c.review.entity.AdminLog;
import com.c2c.review.mapper.AdminLogMapper;
import com.c2c.user.entity.User;
import com.c2c.user.mapper.UserMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 管理端权限分配：管理员查看用户角色列表，并给账号分配「审核员」等角色。
 * 角色分配落库 user.role（0 普通用户 / 1 管理员 / 2 审核员），并写 admin_log 审计。
 * 审核员经普通登录后，通过用户端「审核模块」进入审核工作台。
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@Tag(name = "管理端权限分配", description = "管理员查看用户角色并分配审核员/管理员权限（/admin/** 需管理员 token）")
public class RoleAdminController {

    private final UserMapper userMapper;
    private final AdminLogMapper adminLogMapper;

    @Operation(summary = "用户角色列表", description = "关键字搜索 + 角色筛选 + 分页，返回 id/用户名/昵称/角色/状态")
    @GetMapping(ApiPath.ADMIN_USER_ROLES)
    public R<Map<String, Object>> roles(
            @Parameter(description = "搜索关键字（用户名/昵称/手机号/邮箱）") @RequestParam(required = false) String keyword,
            @Parameter(description = "角色筛选：0普通 1管理员 2审核员") @RequestParam(required = false) Integer role,
            @Parameter(description = "页码，从 1 开始") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "20") int size) {
        LambdaQueryWrapper<User> w = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.trim().isEmpty()) {
            String k = keyword.trim();
            w.and(q -> q.like(User::getUsername, k)
                    .or().like(User::getNickname, k)
                    .or().like(User::getPhone, k)
                    .or().like(User::getEmail, k));
        }
        if (role != null) {
            w.eq(User::getRole, role);
        }
        w.orderByAsc(User::getId);
        Page<User> result = userMapper.selectPage(new Page<>(page, size), w);

        Map<String, Object> data = new HashMap<>();
        data.put("total", result.getTotal());
        data.put("records", result.getRecords().stream().map(u -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", u.getId());
            m.put("username", u.getUsername());
            m.put("nickname", u.getNickname());
            m.put("phone", u.getPhone());
            m.put("email", u.getEmail());
            m.put("role", u.getRole());
            m.put("status", u.getStatus());
            m.put("createdAt", u.getCreatedAt());
            return m;
        }).collect(Collectors.toList()));
        return R.ok(data);
    }

    @Operation(summary = "设置用户角色", description = "body：role 0=普通用户 1=管理员 2=审核员；不能修改自己的角色")
    @PutMapping(ApiPath.ADMIN_USER_ROLE)
    public R<Void> setRole(@Parameter(hidden = true) @RequestHeader("X-User-Id") Long operatorId,
                           @Parameter(description = "目标用户 ID") @PathVariable Long userId,
                           @RequestBody Map<String, Integer> body) {
        Integer role = body.get("role");
        if (role == null || role < 0 || role > 2) {
            throw new BusinessException("角色取值不合法（0/1/2）");
        }
        if (userId.equals(operatorId)) {
            throw new BusinessException("不能修改自己的角色");
        }
        User target = userMapper.selectById(userId);
        if (target == null) {
            throw new BusinessException("目标用户不存在");
        }
        String oldRole = roleText(target.getRole());
        target.setRole(role);
        userMapper.updateById(target);

        AdminLog adminLog = new AdminLog();
        adminLog.setOperatorId(operatorId);
        adminLog.setOperatorRole(1);
        adminLog.setAction("set_role");
        adminLog.setTargetType("user");
        adminLog.setTargetId(userId);
        adminLog.setDetail("角色变更：" + oldRole + " -> " + roleText(role));
        adminLogMapper.insert(adminLog);

        log.info("角色分配：operatorId={}, targetId={}, role={}", operatorId, userId, role);
        return R.ok();
    }

    private String roleText(Integer role) {
        if (role == null) return "未知";
        switch (role) {
            case 1: return "管理员";
            case 2: return "审核员";
            default: return "普通用户";
        }
    }
}
