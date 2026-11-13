-- =============================================================
-- 增量迁移：演示银行卡表 + 活动弹窗表 + 余额券类型 2026-11-13
-- 状态：待在 192.168.1.14 执行；新库由全量脚本(init.sql)包含无需执行
-- 说明：
--   1. user_bank_card 为演示银行卡，未接真实支付，充值/提现均为模拟流程；
--   2. activity_popup 为管理员活动弹窗，可绑定优惠券，有效期跟随券；
--   3. coupon_template.type 新增 3 余额券(领取/发放即加余额)。
-- =============================================================

-- 演示银行卡（未接真实支付，仅课程演示）
CREATE TABLE IF NOT EXISTS `user_bank_card` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL COMMENT '持有人用户ID',
  `card_holder` VARCHAR(50) NOT NULL COMMENT '持卡人姓名',
  `card_no` VARCHAR(30) NOT NULL COMMENT '演示银行卡号(模拟数据, 非真实卡号)',
  `bank_name` VARCHAR(50) DEFAULT NULL COMMENT '开户行',
  `is_default` TINYINT NOT NULL DEFAULT 0 COMMENT '是否默认卡 1是0否',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY `idx_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='演示银行卡(未接真实支付, 仅课程演示, 充值提现均为模拟流程)';

-- 活动弹窗（可绑定优惠券等管理员活动）
CREATE TABLE IF NOT EXISTS `activity_popup` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `title` VARCHAR(100) NOT NULL COMMENT '弹窗标题',
  `content` VARCHAR(1000) DEFAULT NULL COMMENT '弹窗内容',
  `image_url` VARCHAR(500) DEFAULT NULL COMMENT '弹窗配图(可空)',
  `coupon_template_id` BIGINT DEFAULT 0 COMMENT '关联券模板ID, 0表示无关联活动',
  `jump_type` VARCHAR(20) NOT NULL DEFAULT 'none' COMMENT '跳转类型 coupon/product/announcement/url/none',
  `jump_target` VARCHAR(200) DEFAULT NULL COMMENT '跳转目标(券/商品/公告ID 或 链接)',
  `valid_start` DATETIME DEFAULT NULL COMMENT '生效开始',
  `valid_end` DATETIME DEFAULT NULL COMMENT '有效期截止; 绑定券时跟随券有效期',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '1展示 0下架',
  `created_by` BIGINT DEFAULT NULL COMMENT '创建人ID',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='活动弹窗(可绑定优惠券等管理员活动)';

-- 券类型注释补充 3 余额券
ALTER TABLE `coupon_template`
  MODIFY COLUMN `type` TINYINT NOT NULL DEFAULT 1
  COMMENT '券类型 1满减 2折扣 3余额券(领取/发放即加余额)';
