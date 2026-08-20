package com.c2c.common.enums;

import lombok.Getter;

/**
 * 商品状态枚举。
 */
@Getter
public enum ProductStatus {

    /** 在售 */
    ON_SALE(1, "在售"),
    /** 已预订 */
    RESERVED(2, "已预订"),
    /** 已售 */
    SOLD(3, "已售"),
    /** 已下架 */
    OFF_SHELF(4, "已下架"),
    /** 违规下架 */
    BANNED(5, "违规下架");

    private final int code;
    private final String text;

    ProductStatus(int code, String text) {
        this.code = code;
        this.text = text;
    }

    public static String getTextByCode(int code) {
        for (ProductStatus s : values()) {
            if (s.code == code) return s.text;
        }
        return "未知";
    }
}
