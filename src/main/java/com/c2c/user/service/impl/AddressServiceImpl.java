package com.c2c.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.c2c.common.exception.BusinessException;
import com.c2c.user.entity.UserAddress;
import com.c2c.user.mapper.UserAddressMapper;
import com.c2c.user.service.AddressService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 地址服务实现类
 * <p>基于 MyBatis-Plus 实现收货地址的增删改查与默认地址管理，
 * 所有操作均校验地址归属，设置默认地址前会清除该用户的其它默认地址。</p>
 */
@Service
@RequiredArgsConstructor
public class AddressServiceImpl implements AddressService {

    private final UserAddressMapper addressMapper;

    @Override
    public List<UserAddress> listByUserId(Long userId) {
        return addressMapper.selectList(
                new LambdaQueryWrapper<UserAddress>()
                        .eq(UserAddress::getUserId, userId)
                        .orderByDesc(UserAddress::getIsDefault)
                        .orderByDesc(UserAddress::getUpdatedAt));
    }

    @Override
    public UserAddress getById(Long addressId, Long userId) {
        UserAddress address = addressMapper.selectById(addressId);
        if (address == null || !address.getUserId().equals(userId)) {
            throw new BusinessException("地址不存在");
        }
        return address;
    }

    @Override
    @Transactional
    public void add(UserAddress address, Long userId) {
        address.setUserId(userId);
        if (address.getIsDefault() != null && address.getIsDefault() == 1) {
            clearDefault(userId);
        }
        addressMapper.insert(address);
    }

    @Override
    @Transactional
    public void update(UserAddress address, Long userId) {
        UserAddress exist = addressMapper.selectById(address.getId());
        if (exist == null || !exist.getUserId().equals(userId)) {
            throw new BusinessException("地址不存在");
        }
        if (address.getIsDefault() != null && address.getIsDefault() == 1) {
            clearDefault(userId);
        }
        addressMapper.updateById(address);
    }

    @Override
    public void delete(Long addressId, Long userId) {
        UserAddress exist = addressMapper.selectById(addressId);
        if (exist == null || !exist.getUserId().equals(userId)) {
            throw new BusinessException("地址不存在");
        }
        addressMapper.deleteById(addressId);
    }

    @Override
    @Transactional
    public void setDefault(Long addressId, Long userId) {
        UserAddress exist = addressMapper.selectById(addressId);
        if (exist == null || !exist.getUserId().equals(userId)) {
            throw new BusinessException("地址不存在");
        }
        clearDefault(userId);
        exist.setIsDefault(1);
        addressMapper.updateById(exist);
    }

    private void clearDefault(Long userId) {
        addressMapper.update(null,
                new LambdaUpdateWrapper<UserAddress>()
                        .eq(UserAddress::getUserId, userId)
                        .set(UserAddress::getIsDefault, 0));
    }
}



