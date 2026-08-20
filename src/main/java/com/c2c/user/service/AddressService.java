package com.c2c.user.service;

import com.c2c.user.entity.UserAddress;

import java.util.List;

/**
 * 地址服务接口
 * <p>提供用户收货地址的查询、新增、修改、删除以及默认地址设置等核心能力，
 * 所有操作均需传入 userId 以校验地址归属，防止越权访问。</p>
 */
public interface AddressService {

    /** 查询指定用户的所有收货地址（默认地址置顶，按更新时间倒序排列） */
    List<UserAddress> listByUserId(Long userId);

    /** 根据地址ID查询地址，并校验其归属于指定用户 */
    UserAddress getById(Long addressId, Long userId);

    /** 新增收货地址（若标记为默认，先清除该用户原有默认地址） */
    void add(UserAddress address, Long userId);

    /** 修改收货地址（校验归属；若标记为默认，先清除原默认地址） */
    void update(UserAddress address, Long userId);

    /** 删除收货地址（校验归属） */
    void delete(Long addressId, Long userId);

    /** 将指定地址设为默认地址（校验归属，并清除其他默认地址） */
    void setDefault(Long addressId, Long userId);
}



