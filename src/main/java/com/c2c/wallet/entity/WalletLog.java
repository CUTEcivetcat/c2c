package com.c2c.wallet.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 资金流水实体：记录每笔余额变动（充值/支付/退款/到账），双向留痕。
 */
@Data
@TableName("wallet_log")
public class WalletLog {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String type;          // recharge / pay / refund / receive
    private BigDecimal amount;    // 正数收入，负数支出
    private BigDecimal balanceBefore;
    private BigDecimal balanceAfter;
    private Long orderId;
    private String remark;
    private LocalDateTime createdAt;
}