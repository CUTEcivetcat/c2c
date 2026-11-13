package com.c2c.wallet.controller;

import com.c2c.common.constant.ApiPath;
import com.c2c.common.result.R;
import com.c2c.wallet.entity.BankCard;
import com.c2c.wallet.service.BankCardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 演示银行卡接口（需登录）：绑卡 / 解绑 / 设默认 / 我的卡列表。
 */
@RestController
@RequiredArgsConstructor
@Tag(name = "演示银行卡", description = "绑定/管理演示银行卡（未接真实支付，仅演示）")
public class BankCardController {

    private final BankCardService bankCardService;

    @Operation(summary = "我的银行卡列表")
    @GetMapping(ApiPath.BANK_CARD_LIST)
    public R<List<BankCard>> list(@Parameter(hidden = true) @RequestHeader("X-User-Id") Long userId) {
        return R.ok(bankCardService.list(userId));
    }

    @Operation(summary = "绑定演示银行卡", description = "body: {cardHolder, cardNo, bankName}；首张自动默认")
    @PostMapping(ApiPath.BANK_CARD)
    public R<BankCard> add(@Parameter(hidden = true) @RequestHeader("X-User-Id") Long userId,
                           @RequestBody Map<String, String> body) {
        return R.ok(bankCardService.add(userId, body.get("cardHolder"), body.get("cardNo"), body.get("bankName")));
    }

    @Operation(summary = "解绑银行卡")
    @DeleteMapping(ApiPath.BANK_CARD_ID)
    public R<Void> remove(@Parameter(hidden = true) @RequestHeader("X-User-Id") Long userId,
                          @Parameter(description = "卡 ID") @PathVariable Long id) {
        bankCardService.remove(userId, id);
        return R.ok();
    }

    @Operation(summary = "设为默认卡")
    @PutMapping(ApiPath.BANK_CARD_DEFAULT)
    public R<Void> setDefault(@Parameter(hidden = true) @RequestHeader("X-User-Id") Long userId,
                              @Parameter(description = "卡 ID") @PathVariable Long id) {
        bankCardService.setDefault(userId, id);
        return R.ok();
    }
}
