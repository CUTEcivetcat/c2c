-- =============================================================
-- 更新 2026-08-22：首页轮播图 banner 表
-- 用法：mysql -uroot -p c2c < sql/update-20260822-banner.sql
-- =============================================================
CREATE TABLE IF NOT EXISTS `banner` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `title` VARCHAR(100) DEFAULT NULL COMMENT '标题',
  `image_url` VARCHAR(500) NOT NULL COMMENT '图片地址',
  `link_url` VARCHAR(500) DEFAULT NULL COMMENT '跳转链接',
  `sort_order` INT DEFAULT 0 COMMENT '排序权重，越小越靠前',
  `status` TINYINT DEFAULT 1 COMMENT '1启用 0停用',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='首页轮播图';