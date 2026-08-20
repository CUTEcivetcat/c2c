package com.c2c.common.utils;

/**
 * 敏感词检测工具（内置简单词库，用于昵称/内容自动审核拦截）。
 * <p>命中即认为含敏感词，由调用方决定拒绝策略。词库可后续扩展为
 * 数据库/配置文件维护。</p>
 */
public final class SensitiveWordUtils {

    private SensitiveWordUtils() {
    }

    /** 内置敏感词（示例词库，可按需扩充） */
    private static final String[] WORDS = {
            "fuck", "shit", "操", "妈的", "傻逼", "滚",
            "诈骗", "赌博", "色情", "毒品", "枪支",
            "代写", "刷单", "赌博网站", "加微信", "加qq"
    };

    /**
     * 检测文本是否包含敏感词。
     *
     * @return 命中的第一个敏感词；无命中返回 null
     */
    public static String hit(String text) {
        if (text == null || text.isEmpty()) {
            return null;
        }
        String lower = text.toLowerCase();
        for (String w : WORDS) {
            if (lower.contains(w)) {
                return w;
            }
        }
        return null;
    }
}
