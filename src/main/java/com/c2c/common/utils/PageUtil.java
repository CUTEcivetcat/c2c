package com.c2c.common.utils;

/**
 * 分页参数边界工具：统一钳制页码与每页条数，避免传入 0、负数或超大 size 拉全表。
 *
 * @author wzh
 */
public final class PageUtil {

    /** 最小页码 */
    public static final int MIN_PAGE = 1;
    /** 每页条数上限 */
    public static final int MAX_SIZE = 50;
    /** 默认每页条数 */
    public static final int DEFAULT_SIZE = 20;

    private PageUtil() {
    }

    /** 页码小于 1 时回落到第 1 页 */
    public static int normalizePage(int page) {
        return page < MIN_PAGE ? MIN_PAGE : page;
    }

    /** 每页条数：小于 1 用默认值，超过上限截断到上限 */
    public static int normalizeSize(int size) {
        if (size < 1) {
            return DEFAULT_SIZE;
        }
        return Math.min(size, MAX_SIZE);
    }
}
