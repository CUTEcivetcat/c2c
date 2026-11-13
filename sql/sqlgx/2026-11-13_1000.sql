-- =============================================================
-- 增量迁移：用户意见建议反馈表 2026-11-13
-- 状态：待在 192.168.1.14 执行；新库由全量脚本(init.sql)包含无需执行
-- 说明：suggestion 为用户提交的意见建议，管理端查看并回复是否采纳；
--       状态 1待处理 2已采纳 3未采纳，回复意见与回复时间记录在 reply/replied_at。
-- =============================================================

-- 用户意见建议反馈（管理端查看并回复是否采纳）
CREATE TABLE IF NOT EXISTS `suggestion` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL COMMENT '提交人用户ID',
  `content` VARCHAR(1000) NOT NULL COMMENT '建议/反馈内容',
  `contact` VARCHAR(50) DEFAULT NULL COMMENT '联系方式(可空)',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '1待处理 2已采纳 3未采纳',
  `reply` VARCHAR(1000) DEFAULT NULL COMMENT '管理回复意见',
  `replied_by` BIGINT DEFAULT NULL COMMENT '回复人管理员ID',
  `replied_at` DATETIME DEFAULT NULL COMMENT '回复时间',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY `idx_user` (`user_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户意见建议反馈(管理端查看并回复是否采纳)';
