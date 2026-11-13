package com.c2c.suggestion.enums;

import lombok.Getter;

/**
 * 建议反馈状态：1待处理 2已采纳 3未采纳
 */
@Getter
public enum SuggestionStatus {

    PENDING(1, "待处理"),
    ADOPTED(2, "已采纳"),
    REJECTED(3, "未采纳");

    private final int code;
    private final String text;

    SuggestionStatus(int code, String text) {
        this.code = code;
        this.text = text;
    }

    public static String getTextByCode(Integer code) {
        if (code == null) return "未知";
        for (SuggestionStatus s : values()) {
            if (s.code == code) return s.text;
        }
        return "未知";
    }
}
