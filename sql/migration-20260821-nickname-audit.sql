-- =============================================================
-- C2C 增量迁移 2026-08-21：昵称修改审核（nickname_audit 表 + user 加列）
-- 用法：mysql -uroot -p c2c < migration-20260821-nickname-audit.sql
-- =============================================================

-- =============================================================
-- 17. nickname_audit 昵称修改审核表
--     用户修改昵称需审核：提交后待人工审核，通过/拒绝留痕。
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

-- 用户表增加昵称审核字段：待审昵称 + 审核状态（0正常 1审核中）
ALTER TABLE `user` ADD COLUMN IF NOT EXISTS `nickname_pending` VARCHAR(50) DEFAULT NULL COMMENT '待审核昵称' AFTER `nickname`;
ALTER TABLE `user` ADD COLUMN IF NOT EXISTS `nickname_status` TINYINT NOT NULL DEFAULT 0 COMMENT '昵称状态：0正常 1审核中' AFTER `nickname_pending`;