package com.c2c.wallet.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 演示银行卡实体：未接入真实支付/清算，仅课程演示用；充值(银行卡方式)与提现均走模拟流程。
 */
@Data
@TableName("user_bank_card")
public class BankCard {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 持有人用户 ID */
    private Long userId;

    /** 持卡人姓名 */
    private String cardHolder;

    /** 演示银行卡号（模拟数据，非真实卡号） */
    private String cardNo;

    /** 开户行 */
    private String bankName;

    /** 是否默认卡 1是 0否 */
    private Integer isDefault;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
