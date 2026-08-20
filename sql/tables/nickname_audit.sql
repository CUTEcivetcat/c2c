-- =============================================================
-- 新增表：nickname_audit 昵称修改审核表
-- 说明：用户修改昵称需审核：提交后待人工审核，通过/拒绝留痕。
--       同时给 user 表补充昵称审核字段（nickname_pending/nickname_status）。
-- 用法：mysql -uroot -p c2c < tables/nickname_audit.sql
--       （CREATE TABLE IF NOT EXISTS / ADD COLUMN，重复执行安全）
-- =============================================================
CREATE TABLE IF NOT EXISTS `nickname_audit` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL COMMENT '申请人用户ID',
  `old_nickname` VARCHAR(50) DEFAULT NULL COMMENT '原昵称',
  `new_nickname` VARCHAR(50) NOT NULL COMMENT '申请的新昵称',
  `status` TINYINT NOT NULL DEFAULT 0 COMMENT '状态：0待审核 1已通过 2已拒绝',
  `reason` VARCHAR(500) DEFAULT NULL COMMENT '处理说明/拒绝原因',
  `handled_by` BIGINT DEFAULT NULL COMMENT '处理人用户ID（审核员/管理员）',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '申请时间',
  `handled_at` DATETIME DEFAULT NULL COMMENT '处理时间',
  KEY `idx_user` (`user_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='昵称修改审核记录';

-- 用户表补充昵称审核字段（待审昵称 + 审核状态：0正常 1审核中）
-- 注意：MySQL 8.0 不支持 ADD COLUMN IF NOT EXISTS；列已存在时执行会报 Duplicate column，忽略即可
ALTER TABLE `user` ADD COLUMN `nickname_pending` VARCHAR(50) DEFAULT NULL COMMENT '待审核昵称' AFTER `nickname`;
ALTER TABLE `user` ADD COLUMN `nickname_status` TINYINT NOT NULL DEFAULT 0 COMMENT '昵称状态：0正常 1审核中' AFTER `nickname_pending`;

-- =============================================================
-- 演示数据（可选执行）：插入 1 条待审核昵称申请，供审核工作台演示，幂等安全
-- 说明：user_id=1 对应演示用户「小王」（13800000001）
-- =============================================================
INSERT INTO `nickname_audit` (`user_id`, `old_nickname`, `new_nickname`, `status`)
SELECT 1, '小王', '小闲鱼', 0
WHERE NOT EXISTS (SELECT 1 FROM `nickname_audit` WHERE `user_id` = 1 AND `status` = 0);
