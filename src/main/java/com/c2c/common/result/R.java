package com.c2c.common.result;

import lombok.Data;

import java.io.Serializable;

/**
 * 统一响应体：接口统一返回结果的封装（包含 code / message / data / timestamp）。
 */
@Data
public class R<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    private int code;
    private String message;
    private T data;
    private long timestamp;

    private R() {
        this.timestamp = System.currentTimeMillis();
    }

    // ========== 成功 ==========

    public static <T> R<T> ok() {
        R<T> r = new R<>();
        r.code = 200;
        r.message = "success";
        return r;
    }

    public static <T> R<T> ok(T data) {
        R<T> r = ok();
        r.data = data;
        return r;
    }

    // ========== 失败 ==========

    public static <T> R<T> fail(int code, String message) {
        R<T> r = new R<>();
        r.code = code;
        r.message = message;
        return r;
    }

    public static <T> R<T> fail(String message) {
        return fail(500, message);
    }

    // ========== 常用错误 ==========

    public static <T> R<T> badRequest(String message) {
        return fail(400, message);
    }

    public static <T> R<T> unauthorized(String message) {
        return fail(401, message);
    }

    public static <T> R<T> forbidden(String message) {
        return fail(403, message);
    }

    public static <T> R<T> notFound(String message) {
        return fail(404, message);
    }
}



