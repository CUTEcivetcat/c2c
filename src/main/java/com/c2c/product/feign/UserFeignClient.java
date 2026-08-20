package com.c2c.product.feign;

import com.c2c.user.dto.UserVO;
import com.c2c.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 调用用户模块的 Feign 客户端：供商品模块获取卖家等用户公开信息（单体模式下基于 Service 直接实现）。
 */
@Component("productUserFeignClient")
@RequiredArgsConstructor
public class UserFeignClient {
    private final UserService userService;

    /** 获取用户公开信息（昵称、头像、信誉分等）。 */
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

    /** 构造统一成功响应。 */
    private Map<String, Object> ok(Object data) {
        Map<String, Object> map = new HashMap<>();
        map.put("code", 200);
        map.put("message", "success");
        map.put("data", data);
        return map;
    }
}

