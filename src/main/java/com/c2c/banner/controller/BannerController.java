package com.c2c.banner.controller;

import com.c2c.banner.entity.Banner;
import com.c2c.banner.service.BannerService;
import com.c2c.common.constant.ApiPath;
import com.c2c.common.result.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 轮播图（用户端公开）：首页顶部运营位大图。
 */
@RestController
@RequiredArgsConstructor
@Tag(name = "轮播图", description = "首页轮播图（公开只读）")
public class BannerController {

    private final BannerService bannerService;

    @Operation(summary = "轮播图列表（启用中）")
    @GetMapping(ApiPath.BANNER_LIST)
    public R<List<Banner>> list() {
        return R.ok(bannerService.listEnabled());
    }
}