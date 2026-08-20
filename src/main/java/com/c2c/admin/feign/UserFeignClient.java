package com.c2c.admin.feign;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.c2c.user.dto.UserVO;
import com.c2c.user.entity.User;
import com.c2c.user.mapper.UserMapper;
import com.c2c.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 调用用户模块的 Feign 客户端：供管理后台查询用户列表、修改状态与信誉分、统计用户数（单体模式下基于 Service/Mapper 直接实现）。
 */
@Component("adminUserFeignClient")
@RequiredArgsConstructor
public class UserFeignClient {
    private final UserService userService;
    private final UserMapper userMapper;

    /** 获取用户公开信息。 */
    public Map<String, Object> getUserPublicInfo(Long userId) {
        UserVO user = userService.getUserPublicInfo(userId);
        Map<String, Object> data = new HashMap<>();
        if (user != null) {
            data.put("id", user.getId());
            data.put("nickname", user.getNickname());
            data.put("avatarUrl", user.getAvatarUrl());
            data.put("reputationScore", user.getReputationScore());
        }
        return ok(data);
    }

    /** 更新用户信誉分。 */
    public Map<String, Object> updateReputation(Long userId, Map<String, Object> body) {
        User user = userMapper.selectById(userId);
        if (user != null && body.containsKey("reputationScore")) {
            user.setReputationScore(new java.math.BigDecimal(String.valueOf(body.get("reputationScore"))));
            userMapper.updateById(user);
        }
        return ok(null);
    }

    /** 分页查询用户列表，可按手机号、邮箱、昵称关键字过滤。 */
    public Map<String, Object> getUserList(String keyword, int page, int size) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        if (StrUtil.isNotBlank(keyword)) {
            wrapper.and(w -> w.like(User::getPhone, keyword)
                    .or().like(User::getEmail, keyword)
                    .or().like(User::getNickname, keyword));
        }
        wrapper.orderByDesc(User::getCreatedAt);
        Page<User> result = userMapper.selectPage(new Page<User>(page, size), wrapper);
        Map<String, Object> data = new HashMap<>();
        data.put("records", result.getRecords());
        data.put("total", result.getTotal());
        data.put("page", page);
        data.put("size", size);
        return ok(data);
    }

    /** 更新用户状态（启用/禁用）。 */
    public Map<String, Object> updateUserStatus(Long userId, Map<String, Integer> body) {
        User user = userMapper.selectById(userId);
        if (user != null && body.containsKey("status")) {
            user.setStatus(body.get("status"));
            userMapper.updateById(user);
        }
        return ok(null);
    }

    /** 统计用户总数。 */
    public Map<String, Object> countUsers() {
        Map<String, Object> data = new HashMap<>();
        data.put("total", userMapper.selectCount(null));
        return ok(data);
    }

    /** 构造统一成功响应。 */
    private Map<String, Object> ok(Object data) {
        Map<String, Object> map = new HashMap<>();
        map.put("code", 200);
        map.put("message", "success");
        map.put("data", data);
        return map;
    }
}

