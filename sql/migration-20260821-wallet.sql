-- =============================================================
-- C2C 增量迁移 2026-08-21：余额钱包 + 资金流水 + 订单托管
-- 用法：mysql -uroot -p c2c < migration-20260821-wallet.sql
-- =============================================================

-- =============================================================
-- 18. wallet_log 资金流水表
--     记录每笔余额变动：充值/支付/退款/到账，进出双向留痕。
-- =============================================================
CREATE TABLE IF NOT EXISTS `wallet_log` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `type` VARCHAR(20) NOT NULL COMMENT '类型：recharge/pay/refund/receive',
  `amount` DECIMAL(10,2) NOT NULL COMMENT '金额（正数=收入，负数=支出）',
  `balance_before` DECIMAL(10,2) NOT NULL COMMENT '变动前余额',
  `balance_after` DECIMAL(10,2) NOT NULL COMMENT '变动后余额',
  `order_id` BIGINT DEFAULT NULL COMMENT '关联订单ID',
  `remark` VARCHAR(200) DEFAULT NULL COMMENT '备注',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY `idx_user` (`user_id`),
  KEY `idx_order` (`order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='资金流水';

-- 用户表增加余额字段
ALTER TABLE `user` ADD COLUMN IF NOT EXISTS `balance` DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '账户余额' AFTER `reputation_score`;

-- 订单表增加平台托管金额字段
ALTER TABLE `order` ADD COLUMN IF NOT EXISTS `escrow` DECIMAL(10,2) DEFAULT NULL COMMENT '平台托管金额（支付时暂扣，收货后打给卖家）' AFTER `payment_method`;