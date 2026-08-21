package com.c2c.wallet.controller;

import com.c2c.common.constant.ApiPath;
import com.c2c.common.result.R;
import com.c2c.wallet.service.WalletService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 钱包接口：余额查询、模拟充值、交易流水（需登录）。
 */
@RestController
@RequiredArgsConstructor
@Tag(name = "钱包", description = "余额查询、模拟充值、交易流水（需登录）")
public class WalletController {

    private final WalletService walletService;

    @Operation(summary = "钱包概况", description = "返回余额 + 最近 20 条流水")
    @GetMapping(ApiPath.WALLET_PROFILE)
    public R<Map<String, Object>> profile(@Parameter(hidden = true) @RequestHeader("X-User-Id") Long userId) {
        return R.ok(walletService.getProfile(userId));
    }

    @Operation(summary = "模拟充值", description = "body: {amount: 100.00}，直接加余额，单次上限 99999 元")
    @PostMapping(ApiPath.WALLET_RECHARGE)
    public R<Void> recharge(@Parameter(hidden = true) @RequestHeader("X-User-Id") Long userId,
                            @RequestBody Map<String, BigDecimal> body) {
        walletService.recharge(userId, body.get("amount"));
        return R.ok();
    }
}