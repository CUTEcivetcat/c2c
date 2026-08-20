package com.c2c.user.controller;

import com.c2c.common.constant.ApiPath;
import com.c2c.common.result.R;
import com.c2c.user.entity.UserAddress;
import com.c2c.user.service.AddressService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 地址控制器
 */
@RestController
@RequiredArgsConstructor
@Tag(name = "收货地址", description = "当前登录用户的收货地址管理（均需登录）")
public class AddressController {

    private final AddressService addressService;

    /**
     * 获取地址列表
     */
    @Operation(summary = "获取地址列表")
    @GetMapping(ApiPath.ADDRESS)
    public R<List<UserAddress>> list(@Parameter(hidden = true) @RequestHeader("X-User-Id") Long userId) {
        List<UserAddress> list = addressService.listByUserId(userId);
        return R.ok(list);
    }

    /**
     * 获取单个地址
     */
    @Operation(summary = "获取单个地址")
    @GetMapping(ApiPath.ADDRESS_ID)
    public R<UserAddress> get(@Parameter(description = "地址 ID") @PathVariable Long id,
                              @Parameter(hidden = true) @RequestHeader("X-User-Id") Long userId) {
        UserAddress address = addressService.getById(id, userId);
        return R.ok(address);
    }

    /**
     * 新增地址
     */
    @Operation(summary = "新增地址")
    @PostMapping(ApiPath.ADDRESS)
    public R<Void> add(@RequestBody UserAddress address,
                       @Parameter(hidden = true) @RequestHeader("X-User-Id") Long userId) {
        addressService.add(address, userId);
        return R.ok();
    }

    /**
     * 更新地址
     */
    @Operation(summary = "更新地址")
    @PutMapping(ApiPath.ADDRESS_ID)
    public R<Void> update(@Parameter(description = "地址 ID") @PathVariable Long id,
                          @RequestBody UserAddress address,
                          @Parameter(hidden = true) @RequestHeader("X-User-Id") Long userId) {
        address.setId(id);
        addressService.update(address, userId);
        return R.ok();
    }

    /**
     * 删除地址
     */
    @Operation(summary = "删除地址")
    @DeleteMapping(ApiPath.ADDRESS_ID)
    public R<Void> delete(@Parameter(description = "地址 ID") @PathVariable Long id,
                          @Parameter(hidden = true) @RequestHeader("X-User-Id") Long userId) {
        addressService.delete(id, userId);
        return R.ok();
    }

    /**
     * 设置默认地址
     */
    @Operation(summary = "设置默认地址")
    @PutMapping(ApiPath.ADDRESS_DEFAULT)
    public R<Void> setDefault(@Parameter(description = "地址 ID") @PathVariable Long id,
                              @Parameter(hidden = true) @RequestHeader("X-User-Id") Long userId) {
        addressService.setDefault(id, userId);
        return R.ok();
    }
}
