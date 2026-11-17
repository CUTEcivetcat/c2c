-- =============================================================
-- 增量迁移：商品列表「状态 + 发布时间」复合索引 2026-11-17
-- 背景：首页/列表按 status=在售 过滤并按 created_at 倒序，
--       原索引 (category_id, status) 不含时间，纯首页排序易 filesort。
-- 状态：待在目标库执行；新库由全量 init.sql 已包含该索引，无需重复执行。
-- 幂等：索引已存在时跳过，可重复执行。
-- =============================================================

SET @exist := (
    SELECT COUNT(*) FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'product'
      AND INDEX_NAME = 'idx_status_created'
);

SET @sql := IF(@exist = 0,
    'ALTER TABLE `product` ADD INDEX `idx_status_created` (`status`, `created_at`)',
    'SELECT 1');

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
