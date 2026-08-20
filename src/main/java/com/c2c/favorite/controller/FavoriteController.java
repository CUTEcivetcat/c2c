package com.c2c.favorite.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.c2c.common.constant.ApiPath;
import com.c2c.common.result.R;
import com.c2c.favorite.entity.Favorite;
import com.c2c.favorite.service.FavoriteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 商品收藏：添加 / 取消收藏 / 收藏列表 / 是否已收藏。均需登录。
 */
@RestController
@RequiredArgsConstructor
@Tag(name = "商品收藏", description = "收藏 / 取消收藏 / 收藏列表 / 收藏状态（均需登录）")
public class FavoriteController {

    private final FavoriteService favoriteService;

    @Operation(summary = "收藏商品")
    @PostMapping(ApiPath.FAVORITE_ID)
    public R<Void> add(@Parameter(description = "商品 ID") @PathVariable Long productId,
                       @Parameter(hidden = true) @RequestHeader("X-User-Id") Long userId) {
        favoriteService.add(userId, productId);
        return R.ok();
    }

    @Operation(summary = "取消收藏")
    @DeleteMapping(ApiPath.FAVORITE_ID)
    public R<Void> remove(@Parameter(description = "商品 ID") @PathVariable Long productId,
                          @Parameter(hidden = true) @RequestHeader("X-User-Id") Long userId) {
        favoriteService.remove(userId, productId);
        return R.ok();
    }

    @Operation(summary = "收藏列表（分页）")
    @GetMapping(ApiPath.FAVORITE_LIST)
    public R<Page<Favorite>> list(@Parameter(hidden = true) @RequestHeader("X-User-Id") Long userId,
                                  @Parameter(description = "页码，从 1 开始") @RequestParam(defaultValue = "1") int page,
                                  @Parameter(description = "每页条数") @RequestParam(defaultValue = "20") int size) {
        return R.ok(favoriteService.list(userId, page, size));
    }

    @Operation(summary = "是否已收藏某商品")
    @GetMapping(ApiPath.FAVORITE_CHECK)
    public R<Map<String, Boolean>> check(@Parameter(description = "商品 ID") @PathVariable Long productId,
                                         @Parameter(hidden = true) @RequestHeader("X-User-Id") Long userId) {
        return R.ok(com.c2c.common.utils.MapUtils.of("isFavorited", favoriteService.isFavorited(userId, productId)));
    }
}
