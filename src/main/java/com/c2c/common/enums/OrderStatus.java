package com.c2c.common.enums;

import lombok.Getter;

/**
 * 订单状态枚举。
 */
@Getter
public enum OrderStatus {

    /** 待支付 */
    PENDING_PAYMENT(0, "待支付"),
    /** 已支付 */
    PAID(1, "已支付"),
    /** 已发货 */
    SHIPPED(2, "已发货"),
    /** 已收货 */
    RECEIVED(3, "已收货"),
    /** 已完成 */
    COMPLETED(4, "已完成"),
    /** 已取消 */
    CANCELLED(5, "已取消");

    private final int code;
    private final String text;

    OrderStatus(int code, String text) {
        this.code = code;
        this.text = text;
    }

    public static String getTextByCode(int code) {
        for (OrderStatus status : values()) {
            if (status.code == code) return status.text;
        }
        return "未知";
    }
}
