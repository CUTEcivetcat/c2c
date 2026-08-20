-- =============================================================
-- 新增表：announcement 平台公告表
-- 说明：管理员发布公告/平台公约/通知，用户端公告页与首页横幅展示。
-- 用法：mysql -uroot -p c2c < tables/announcement.sql
--       （CREATE TABLE IF NOT EXISTS，重复执行安全）
-- =============================================================
CREATE TABLE IF NOT EXISTS `announcement` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `title` VARCHAR(200) NOT NULL COMMENT '公告标题',
  `content` TEXT NOT NULL COMMENT '公告内容',
  `type` TINYINT NOT NULL DEFAULT 1 COMMENT '类型：1公告 2平台公约 3通知',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1已发布 0已下架',
  `pinned` TINYINT NOT NULL DEFAULT 0 COMMENT '是否置顶：1置顶 0普通',
  `created_by` BIGINT DEFAULT NULL COMMENT '发布人用户ID',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发布时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  KEY `idx_status_type` (`status`, `type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='平台公告';
