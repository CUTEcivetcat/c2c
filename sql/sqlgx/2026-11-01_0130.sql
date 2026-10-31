-- =============================================================
-- 增量迁移：令牌版本化 + 订单号唯一索引（M1，2026-10-31 代码已依赖）
-- 适用：已有数据、不方便重灌全量脚本的库（192.168.1.14 / 192.168.1.11 均已执行）
-- 幂等版：MariaDB 语法，重复执行不报错；纯 MySQL 8 需去掉 IF [NOT] EXISTS
-- 新库无需执行（init.sql 全量脚本已包含）
-- =============================================================

-- ① user 表加 token_version：改密/封禁/角色权限变更时递增，使旧 token 全部失效
ALTER TABLE `user` ADD COLUMN IF NOT EXISTS `token_version` INT NOT NULL DEFAULT 0
  COMMENT '令牌版本：改密/封禁/角色权限变更时递增，使旧token全部失效' AFTER `admin_perms`;

-- ② order_no 唯一索引：订单号查重 + 防重复（先删旧普通索引）
ALTER TABLE `order` DROP INDEX IF EXISTS `idx_order_no`;
ALTER TABLE `order` ADD UNIQUE INDEX IF NOT EXISTS `uk_order_no` (`order_no`);

-- 验证：
-- SHOW COLUMNS FROM `user` LIKE 'token_version';
-- SHOW INDEX FROM `order` WHERE Key_name = 'uk_order_no';
