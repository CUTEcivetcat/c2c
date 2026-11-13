-- =============================================================
-- 增量迁移：M4-1 商品图片缩略图（product_image 增加 thumb_url 列）2026-11-12
-- 状态：待在 192.168.1.14 执行；新库由全量脚本(init.sql)包含无需执行
-- 说明：thumb_url 存 400px 宽缩略图地址；老数据该列为 NULL，前端回退原图
-- =============================================================
ALTER TABLE `product_image`
  ADD COLUMN IF NOT EXISTS `thumb_url` VARCHAR(500) DEFAULT NULL COMMENT '400px缩略图地址' AFTER `url`;
