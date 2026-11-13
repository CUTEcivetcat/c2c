-- =============================================================
-- 增量迁移：G6 优惠券（券模板 + 用户券 + 订单抵扣字段）2026-11-12
-- 状态：待在 192.168.1.14 执行；新库由全量脚本(init.sql)包含无需执行
-- 说明：满减券；coupon_template 为发行批次，user_coupon 为用户领取记录；
--       order.coupon_id 回填 user_coupon.id，discount_amount 为抵扣快照
-- =============================================================

CREATE TABLE IF NOT EXISTS `coupon_template` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `name` VARCHAR(100) NOT NULL COMMENT '券名称',
  `type` TINYINT NOT NULL DEFAULT 1 COMMENT '券类型 1满减(预留折扣)',
  `discount_amount` DECIMAL(10,2) NOT NULL COMMENT '满减金额',
  `min_amount` DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '使用门槛(订单金额满此额可用)，0无门槛',
  `total_count` INT NOT NULL DEFAULT 0 COMMENT '发行总量',
  `claimed_count` INT NOT NULL DEFAULT 0 COMMENT '已领取数',
  `valid_start` DATETIME DEFAULT NULL COMMENT '生效开始',
  `valid_end` DATETIME DEFAULT NULL COMMENT '有效期截止',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '1生效 0停用',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='优惠券模板(批次)';

CREATE TABLE IF NOT EXISTS `user_coupon` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `coupon_id` BIGINT NOT NULL COMMENT '券模板ID(coupon_template.id)',
  `user_id` BIGINT NOT NULL COMMENT '持有人',
  `status` TINYINT NOT NULL DEFAULT 0 COMMENT '0未使用 1已使用 2已过期 3已作废',
  `order_id` BIGINT DEFAULT NULL COMMENT '使用后回填订单ID',
  `discount_amount` DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '本券抵扣金额(快照)',
  `receive_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `use_time` DATETIME DEFAULT NULL,
  `expire_time` DATETIME DEFAULT NULL COMMENT '过期时间',
  UNIQUE KEY `uk_coupon_user` (`coupon_id`, `user_id`),
  KEY `idx_user_status` (`user_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户已领优惠券';

ALTER TABLE `order`
  ADD COLUMN IF NOT EXISTS `coupon_id` BIGINT DEFAULT NULL COMMENT '使用的用户券ID(user_coupon.id)' AFTER `escrow`,
  ADD COLUMN IF NOT EXISTS `discount_amount` DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '优惠券抵扣金额' AFTER `coupon_id`;
