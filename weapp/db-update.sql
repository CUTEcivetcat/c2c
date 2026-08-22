-- =============================================================
-- 微信小程序数据库变更（暂不部署到生产）
-- 说明：本地运行需要，已应用到开发服务器库。生产部署前需执行。
-- =============================================================
USE c2c;

-- 1. 用户表加微信 openid（唯一）
ALTER TABLE `user` ADD COLUMN `openid` VARCHAR(64) DEFAULT NULL COMMENT '微信openid' AFTER `email`;
ALTER TABLE `user` ADD UNIQUE INDEX `uk_openid` (`openid`); '微信openid' AFTER `email`;
ALTER TABLE `user` ADD UNIQUE INDEX `uk_openid` (`openid`);

-- 2. 用户表加登录来源（email/wechat/phone）
ALTER TABLE `user` ADD COLUMN `login_source` VARCHAR(20) DEFAULT 'email' COMMENT '注册来源：email/wechat/phone' AFTER `role`;