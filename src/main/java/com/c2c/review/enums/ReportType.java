package com.c2c.review.enums;

import lombok.Getter;

/**
 * 举报类型：1违禁品 2假冒伪劣 3描述不符 4欺诈 5侵权 6其他
 */
@Getter
public enum ReportType {

    PROHIBITED(1, "违禁品"),
    COUNTERFEIT(2, "假冒伪劣"),
    DESCRIPTION_MISMATCH(3, "描述不符"),
    FRAUD(4, "欺诈"),
    INFRINGEMENT(5, "侵权"),
    OTHER(6, "其他");

    private final int code;
    private final String text;

    ReportType(int code, String text) {
        this.code = code;
        this.text = text;
    }

    public static String getTextByCode(Integer code) {
        if (code == null) return "未知";
        for (ReportType t : values()) {
            if (t.code == code) return t.text;
        }
        return "未知";
    }
}
