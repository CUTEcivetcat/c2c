package com.c2c.common.enums;

import lombok.Getter;

/**
 * 商品成色枚举
 */
@Getter
public enum ProductCondition {

    BRAND_NEW(1, "全新"),
    LIKE_NEW(2, "几乎全新"),
    LIGHTLY_USED(3, "轻微使用"),
    HEAVILY_USED(4, "明显使用");

    private final int code;
    private final String text;

    ProductCondition(int code, String text) {
        this.code = code;
        this.text = text;
    }

    public static String getTextByCode(int code) {
        for (ProductCondition c : values()) {
            if (c.code == code) return c.text;
        }
        return "未知";
    }
}



