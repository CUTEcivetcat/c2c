package com.c2c.review.enums;

import lombok.Getter;

/**
 * 整改申诉状态：1待审核 2已通过（恢复上架） 3已驳回
 */
@Getter
public enum AppealStatus {

    PENDING(1, "待审核"),
    APPROVED(2, "已通过"),
    REJECTED(3, "已驳回");

    private final int code;
    private final String text;

    AppealStatus(int code, String text) {
        this.code = code;
        this.text = text;
    }

    public static String getTextByCode(Integer code) {
        if (code == null) return "未知";
        for (AppealStatus s : values()) {
            if (s.code == code) return s.text;
        }
        return "未知";
    }
}
