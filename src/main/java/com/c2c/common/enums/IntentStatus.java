package com.c2c.common.enums;

import lombok.Getter;

/**
 * 购买意向状态：1待处理 2已回复 3已成交 4已关闭
 */
@Getter
public enum IntentStatus {

    PENDING(1, "待处理"),
    REPLIED(2, "已回复"),
    DEAL(3, "已成交"),
    CLOSED(4, "已关闭");

    private final int code;
    private final String text;

    IntentStatus(int code, String text) {
        this.code = code;
        this.text = text;
    }

    public static String getTextByCode(int code) {
        for (IntentStatus s : values()) {
            if (s.code == code) return s.text;
        }
        return "未知";
    }

}
