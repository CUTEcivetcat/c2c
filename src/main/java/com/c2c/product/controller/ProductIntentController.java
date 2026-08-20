package com.c2c.product.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.c2c.common.constant.ApiPath;
import com.c2c.common.result.R;
import com.c2c.product.dto.IntentCreateDTO;
import com.c2c.product.dto.IntentVO;
import com.c2c.product.service.ProductIntentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.Map;

/**
 * 购买意向（“我想要”，询价/砍价）：买家表达意向、买家查看我发出的、
 * 卖家查看收到的、卖家回复、买卖双方关闭、卖家标记成交。均需登录。
 */
@RestController
@RequiredArgsConstructor
@Tag(name = "购买意向", description = "“我想要”询价/砍价：发起 / 回复 / 关闭 / 标记成交（均需登录）")
public class ProductIntentController {

    private final ProductIntentService intentService;

    /** 买家表达购买意向 */
    @Operation(summary = "买家表达购买意向", description = "对某商品发起询价/砍价")
    @PostMapping(ApiPath.INTENT_CREATE)
    public R<Long> create(@Parameter(description = "商品 ID") @PathVariable Long productId,
                          @Valid @RequestBody IntentCreateDTO dto,
                          @Parameter(hidden = true) @RequestHeader("X-User-Id") Long userId) {
        return R.ok(intentService.create(productId, dto, userId));
    }

    /** 买家：我发出的意向 */
    @Operation(summary = "我发出的意向（买家）")
    @GetMapping(ApiPath.INTENT_MY)
    public R<Page<IntentVO>> my(@Parameter(hidden = true) @RequestHeader("X-User-Id") Long userId,
                                @Parameter(description = "页码，从 1 开始") @RequestParam(defaultValue = "1") int page,
                                @Parameter(description = "每页条数") @RequestParam(defaultValue = "20") int size) {
        return R.ok(intentService.myList(userId, page, size));
    }

    /** 卖家：收到的意向 */
    @Operation(summary = "收到的意向（卖家）")
    @GetMapping(ApiPath.INTENT_SELLER)
    public R<Page<IntentVO>> seller(@Parameter(hidden = true) @RequestHeader("X-User-Id") Long userId,
                                    @Parameter(description = "页码，从 1 开始") @RequestParam(defaultValue = "1") int page,
                                    @Parameter(description = "每页条数") @RequestParam(defaultValue = "20") int size) {
        return R.ok(intentService.sellerList(userId, page, size));
    }

    /** 卖家回复意向 */
    @Operation(summary = "卖家回复意向", description = "body 传 reply 回复内容")
    @PutMapping(ApiPath.INTENT_REPLY)
    public R<Void> reply(@Parameter(description = "意向 ID") @PathVariable Long id,
                         @RequestBody Map<String, String> body,
                         @Parameter(hidden = true) @RequestHeader("X-User-Id") Long userId) {
        intentService.reply(id, body.get("reply"), userId);
        return R.ok();
    }

    /** 买家或卖家关闭意向 */
    @Operation(summary = "关闭意向")
    @PutMapping(ApiPath.INTENT_CLOSE)
    public R<Void> close(@Parameter(description = "意向 ID") @PathVariable Long id,
                         @Parameter(hidden = true) @RequestHeader("X-User-Id") Long userId) {
        intentService.close(id, userId);
        return R.ok();
    }

    /** 卖家标记成交 */
    @Operation(summary = "标记成交", description = "卖家将意向标记为已成交")
    @PutMapping(ApiPath.INTENT_DEAL)
    public R<Void> deal(@Parameter(description = "意向 ID") @PathVariable Long id,
                        @Parameter(hidden = true) @RequestHeader("X-User-Id") Long userId) {
        intentService.deal(id, userId);
        return R.ok();
    }
}
