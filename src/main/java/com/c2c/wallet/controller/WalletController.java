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

    @Operation(summary = "银行卡方式模拟充值", description = "body: {amount, cardId?}；未指定卡走默认演示卡，未绑卡提示先绑卡")
    @PostMapping(ApiPath.WALLET_RECHARGE_CARD)
    public R<Void> rechargeByCard(@Parameter(hidden = true) @RequestHeader("X-User-Id") Long userId,
                                  @RequestBody Map<String, Object> body) {
        BigDecimal amount = new BigDecimal(body.get("amount").toString());
        Long cardId = body.get("cardId") == null ? null : Long.valueOf(body.get("cardId").toString());
        walletService.rechargeByCard(userId, amount, cardId);
        return R.ok();
    }

    @Operation(summary = "生成充值二维码票据", description = "返回一次性短期 ticket，供手机 H5 扫码充值")
    @GetMapping(ApiPath.WALLET_RECHARGE_TICKET)
    public R<Map<String, String>> ticket(@Parameter(hidden = true) @RequestHeader("X-User-Id") Long userId) {
        String t = walletService.createRechargeTicket(userId);
        return R.ok(Map.of("ticket", t));
    }

    @Operation(summary = "H5 凭票据充值", description = "公开接口，body: {ticket, amount}；票据一次性、5 分钟有效")
    @PostMapping(ApiPath.WALLET_RECHARGE_BY_TICKET)
    public R<Void> rechargeByTicket(@RequestBody Map<String, Object> body) {
        String ticket = String.valueOf(body.get("ticket"));
        BigDecimal amount = new BigDecimal(body.get("amount").toString());
        walletService.rechargeByTicket(ticket, amount);
        return R.ok();
    }

    @Operation(summary = "模拟提现", description = "body: {amount, cardId?}；扣余额到演示卡，未绑卡/余额不足拦截")
    @PostMapping(ApiPath.WALLET_WITHDRAW)
    public R<Void> withdraw(@Parameter(hidden = true) @RequestHeader("X-User-Id") Long userId,
                            @RequestBody Map<String, Object> body) {
        BigDecimal amount = new BigDecimal(body.get("amount").toString());
        Long cardId = body.get("cardId") == null ? null : Long.valueOf(body.get("cardId").toString());
        walletService.withdraw(userId, amount, cardId);
        return R.ok();
    }
}