package com.c2c.common.ratelimit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 接口限流注解：标注在 Controller 方法上，按「用户 ID（未登录按 IP）+ 接口」
 * 在指定时间窗口内计数，超限返回 429。基于 Redis 计数，多实例共享配额。
 *
 * @author wzh
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimit {

    /** 窗口内最大请求次数 */
    int limit() default 10;

    /** 时间窗口（秒） */
    int windowSec() default 60;

    /** 超限提示语 */
    String message() default "操作过于频繁，请稍后再试";
}
