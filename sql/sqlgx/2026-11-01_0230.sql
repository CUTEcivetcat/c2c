-- =============================================================
-- 数据修复：login_source 登录来源纠正（2026-11-01 发现）
-- 背景：该字段默认 'email'，注册/验证码自动注册代码未赋值，导致手机号注册用户来源全错
-- 规则：有 openid → wechat；有邮箱无手机 → email；有手机 → phone
-- 状态：已在 192.168.1.14 执行
-- =============================================================
UPDATE `user`
SET `login_source` = CASE
    WHEN `openid` IS NOT NULL AND `openid` <> '' THEN 'wechat'
    WHEN `email` IS NOT NULL AND `email` <> '' AND (`phone` IS NULL OR `phone` = '') THEN 'email'
    WHEN `phone` IS NOT NULL AND `phone` <> '' THEN 'phone'
    ELSE 'email'
END;

-- 验证：SELECT id, username, phone, email, openid, login_source FROM user ORDER BY id;
