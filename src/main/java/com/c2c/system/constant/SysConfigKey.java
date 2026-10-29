package com.c2c.system.constant;

/**
 * 系统运行配置键（sys_config 表 config_key 的集中定义）。
 *
 * <p>配置值由管理端「系统设置」面板动态调整，后端运行时读取；
 * 新增可配置项时：在此加常量 → init.sql/data.sql 加种子行 → 控制面板加交互。</p>
 */
public interface SysConfigKey {

    /** 验证码是否在接口响应中回显（仅本地/演示环境允许开启，生产必须 false） */
    String SMS_ECHO_CODE = "sys.sms-echo-code";

    /** 管理员登录失败锁定开关 */
    String ADMIN_LOGIN_LOCK_ENABLED = "security.admin-login-lock.enabled";

    /** 管理员登录连续失败次数上限（达到后锁定） */
    String ADMIN_LOGIN_LOCK_MAX_ATTEMPTS = "security.admin-login-lock.max-attempts";

    /** 管理员登录锁定时长（分钟） */
    String ADMIN_LOGIN_LOCK_LOCK_MINUTES = "security.admin-login-lock.lock-minutes";
}
