-- =============================================================
-- 增量迁移：折扣券减免封顶 2026-11-13
-- 状态：待在 192.168.1.14 执行；新库由全量脚本(init.sql)包含无需执行
-- 说明：coupon_template 增加 max_discount_amount，折扣券减免额封顶，0 表示不封顶；满减券不使用该字段
-- =============================================================

ALTER TABLE `coupon_template`
  ADD COLUMN IF NOT EXISTS `max_discount_amount` DECIMAL(10,2) NOT NULL DEFAULT 0
  COMMENT '折扣券减免封顶金额，0不封顶' AFTER `category_id`;
