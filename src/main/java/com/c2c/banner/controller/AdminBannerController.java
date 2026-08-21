package com.c2c.banner.controller;

import com.c2c.banner.entity.Banner;
import com.c2c.banner.service.BannerService;
import com.c2c.common.constant.ApiPath;
import com.c2c.common.result.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 轮播图管理（管理员）：新增 / 编辑 / 删除 / 启停。
 */
@RestController
@RequiredArgsConstructor
@Tag(name = "轮播图管理", description = "管理员配置首页轮播图")
public class AdminBannerController {

    private final BannerService bannerService;

    @Operation(summary = "轮播图全部列表")
    @GetMapping(ApiPath.BANNER_ADMIN_LIST)
    public R<List<Banner>> list() {
        return R.ok(bannerService.listAll());
    }

    @Operation(summary = "新增轮播图")
    @PostMapping(ApiPath.BANNER_ADMIN)
    public R<Banner> create(@RequestBody Banner banner) {
        return R.ok(bannerService.create(banner));
    }

    @Operation(summary = "编辑轮播图")
    @PutMapping(ApiPath.BANNER_ADMIN_ID)
    public R<Banner> update(@Parameter(description = "ID") @PathVariable Long id, @RequestBody Banner banner) {
        return R.ok(bannerService.update(id, banner));
    }

    @Operation(summary = "删除轮播图")
    @DeleteMapping(ApiPath.BANNER_ADMIN_ID)
    public R<Void> delete(@Parameter(description = "ID") @PathVariable Long id) {
        bannerService.delete(id);
        return R.ok();
    }
}