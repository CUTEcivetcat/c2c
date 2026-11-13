-- =============================================================
-- 增量迁移：G6 优惠券适用分类 2026-11-12
-- 状态：待在 192.168.1.14 执行；新库由全量脚本(init.sql)包含无需执行
-- 说明：coupon_template 增加 category_id，0 表示全部分类通用，否则绑定单个商品分类
-- =============================================================

ALTER TABLE `coupon_template`
  ADD COLUMN IF NOT EXISTS `category_id` BIGINT NOT NULL DEFAULT 0
  COMMENT '适用分类ID(category.id)，0全部分类通用' AFTER `min_amount`;
