package com.c2c.order.feign;

import com.c2c.user.entity.UserAddress;
import com.c2c.user.mapper.UserAddressMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 调用用户模块的 Feign 客户端：供订单模块获取用户收货地址（单体模式下基于 Mapper 直接实现）。
 */
@Component("orderUserFeignClient")
@RequiredArgsConstructor
public class UserFeignClient {
    private final UserAddressMapper userAddressMapper;

    /** 获取用户收货地址，并校验地址归属当前用户。 */
    public Map<String, Object> getAddress(Long addressId, Long userId) {
        UserAddress address = userAddressMapper.selectById(addressId);
        Map<String, Object> data = null;
        if (address != null && address.getUserId().equals(userId)) {
            data = new HashMap<>();
            data.put("id", address.getId());
            data.put("receiverName", address.getReceiverName());
            data.put("phone", address.getPhone());
            data.put("province", address.getProvince());
            data.put("city", address.getCity());
            data.put("district", address.getDistrict());
            data.put("detail", address.getDetail());
            data.put("postalCode", address.getPostalCode());
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

