package com.c2c.product.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 购买意向（询价/留言）创建请求参数对象
 */
@Data
public class IntentCreateDTO {

    /** 买家留言/询价（可选） */
    private String message;

    /** 买家期望价格（可选） */
    private BigDecimal expectedPrice;
}
