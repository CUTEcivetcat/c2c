package com.c2c.rating.delegate;

import com.c2c.user.entity.User;
import com.c2c.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * 跨模块委派组件（单体模式直调）：供评分模块回写用户信誉分。
 */
@Component("ratingUserDelegate")
@RequiredArgsConstructor
public class UserDelegate {
    private final UserMapper userMapper;

    /** 更新用户信誉分（评分产生后回写）。 */
    public Map<String, Object> updateReputation(Long userId, Map<String, BigDecimal> body) {
        User user = userMapper.selectById(userId);
        if (user != null && body.containsKey("reputationScore")) {
            user.setReputationScore(body.get("reputationScore"));
            userMapper.updateById(user);
        }
        Map<String, Object> map = new HashMap<>();
        map.put("code", 200);
        map.put("message", "success");
        map.put("data", null);
        return map;
    }
}
