package com.c2c.review.enums;

import lombok.Getter;

/**
 * 举报状态：1待处理 2已违规下架 3已驳回
 */
@Getter
public enum ReportStatus {

    PENDING(1, "待处理"),
    BANNED(2, "已违规下架"),
    REJECTED(3, "已驳回");

    private final int code;
    private final String text;

    ReportStatus(int code, String text) {
        this.code = code;
        this.text = text;
    }

    public static String getTextByCode(Integer code) {
        if (code == null) return "未知";
        for (ReportStatus s : values()) {
            if (s.code == code) return s.text;
        }
        return "未知";
    }
}
