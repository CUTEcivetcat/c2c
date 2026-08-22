-- =============================================================
-- C2C 全量建表脚本（init.sql）
-- 用法：mysql -uroot -p c2c < init.sql   （会清空重建 c2c 库）
-- =============================================================
SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;
DROP DATABASE IF EXISTS c2c;
CREATE DATABASE c2c DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE c2c;

-- ==================== 基础表 15 张 ====================

CREATE TABLE `user` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `username` VARCHAR(100) NOT NULL,
  `password` VARCHAR(255) NOT NULL,
  `phone` VARCHAR(20) UNIQUE,
  `email` VARCHAR(100),
  `email_verified` TINYINT DEFAULT 0,
  `avatar_url` VARCHAR(500),
  `nickname` VARCHAR(50),
  `nickname_pending` VARCHAR(50) DEFAULT NULL,
  `nickname_status` TINYINT NOT NULL DEFAULT 0,
  `bio` TEXT,
  `gender` TINYINT DEFAULT 0,
  `status` TINYINT DEFAULT 1,
  `role` TINYINT DEFAULT 0,
  `reputation_score` DECIMAL(3,1) DEFAULT 5.0,
  `balance` DECIMAL(10,2) NOT NULL DEFAULT 0.00,
  `last_login_at` DATETIME,
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX `idx_phone` (`phone`),
  INDEX `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `user_address` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL,
  `receiver_name` VARCHAR(50) NOT NULL,
  `phone` VARCHAR(20) NOT NULL,
  `province` VARCHAR(50),
  `city` VARCHAR(50),
  `district` VARCHAR(50),
  `detail` VARCHAR(255) NOT NULL,
  `postal_code` VARCHAR(10),
  `is_default` TINYINT DEFAULT 0,
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `category` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `name` VARCHAR(50) NOT NULL,
  `parent_id` BIGINT DEFAULT 0,
  `level` TINYINT DEFAULT 1,
  `sort_order` INT DEFAULT 0,
  `icon_url` VARCHAR(500),
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  INDEX `idx_parent_id` (`parent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `product` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `seller_id` BIGINT NOT NULL,
  `category_id` BIGINT NOT NULL,
  `title` VARCHAR(200) NOT NULL,
  `description` TEXT,
  `price` DECIMAL(10,2) NOT NULL,
  `original_price` DECIMAL(10,2),
  `condition` TINYINT NOT NULL,
  `status` TINYINT DEFAULT 1,
  `review_reason` VARCHAR(500) DEFAULT NULL,
  `freight_type` TINYINT DEFAULT 1,
  `freight_amount` DECIMAL(10,2) DEFAULT 0,
  `view_count` INT DEFAULT 0,
  `favorite_count` INT DEFAULT 0,
  `location` VARCHAR(100),
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX `idx_seller_id` (`seller_id`),
  INDEX `idx_category_status` (`category_id`, `status`),
  INDEX `idx_price` (`price`),
  INDEX `idx_view_count` (`view_count`),
  FULLTEXT INDEX `ft_search` (`title`, `description`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `product_image` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `product_id` BIGINT NOT NULL,
  `url` VARCHAR(500) NOT NULL,
  `sort_order` INT DEFAULT 0,
  `is_cover` TINYINT DEFAULT 0,
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  INDEX `idx_product_id` (`product_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `order` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `order_no` VARCHAR(32) NOT NULL UNIQUE,
  `buyer_id` BIGINT NOT NULL,
  `seller_id` BIGINT NOT NULL,
  `product_id` BIGINT NOT NULL,
  `product_title` VARCHAR(200) NOT NULL,
  `product_image` VARCHAR(500),
  `price` DECIMAL(10,2) NOT NULL,
  `freight_amount` DECIMAL(10,2) DEFAULT 0,
  `total_amount` DECIMAL(10,2) NOT NULL,
  `address_id` BIGINT NOT NULL,
  `address_snapshot` JSON,
  `status` TINYINT NOT NULL DEFAULT 0,
  `payment_method` VARCHAR(20),
  `escrow` DECIMAL(10,2) DEFAULT NULL,
  `payment_time` DATETIME,
  `ship_company` VARCHAR(50),
  `ship_no` VARCHAR(50),
  `ship_time` DATETIME,
  `receive_time` DATETIME,
  `complete_time` DATETIME,
  `cancel_time` DATETIME,
  `cancel_reason` VARCHAR(500),
  `buyer_rated` TINYINT DEFAULT 0,
  `seller_rated` TINYINT DEFAULT 0,
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX `idx_order_no` (`order_no`),
  INDEX `idx_buyer_status` (`buyer_id`, `status`),
  INDEX `idx_seller_status` (`seller_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `favorite` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL,
  `product_id` BIGINT NOT NULL,
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  UNIQUE INDEX `uk_user_product` (`user_id`, `product_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `rating` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `order_id` BIGINT NOT NULL,
  `rater_id` BIGINT NOT NULL,
  `rated_user_id` BIGINT NOT NULL,
  `role` TINYINT NOT NULL,
  `score` TINYINT NOT NULL,
  `comment` VARCHAR(500),
  `tags` JSON,
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  UNIQUE INDEX `uk_order_rater` (`order_id`, `rater_id`),
  INDEX `idx_rated_user` (`rated_user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `conversation` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `user1_id` BIGINT NOT NULL,
  `user2_id` BIGINT NOT NULL,
  `product_id` BIGINT,
  `last_message` VARCHAR(500),
  `last_message_time` DATETIME,
  `user1_unread` INT DEFAULT 0,
  `user2_unread` INT DEFAULT 0,
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE INDEX `uk_user_product` (`user1_id`, `user2_id`, `product_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `message` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `conversation_id` BIGINT NOT NULL,
  `sender_id` BIGINT NOT NULL,
  `receiver_id` BIGINT NOT NULL,
  `content` TEXT NOT NULL,
  `message_type` TINYINT DEFAULT 1,
  `extra` JSON,
  `is_read` TINYINT DEFAULT 0,
  `created_at` DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3),
  INDEX `idx_conversation` (`conversation_id`, `created_at`),
  INDEX `idx_receiver_unread` (`receiver_id`, `is_read`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `product_comment` (
  `id` BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  `product_id` BIGINT NOT NULL,
  `user_id` BIGINT NOT NULL,
  `parent_id` BIGINT NOT NULL DEFAULT 0,
  `content` VARCHAR(1000) NOT NULL,
  `status` TINYINT NOT NULL DEFAULT 1,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY `idx_product` (`product_id`, `status`),
  KEY `idx_parent` (`parent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `product_intent` (
  `id` BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  `product_id` BIGINT NOT NULL,
  `seller_id` BIGINT NOT NULL,
  `buyer_id` BIGINT NOT NULL,
  `message` VARCHAR(500) DEFAULT NULL,
  `expected_price` DECIMAL(10,2) DEFAULT NULL,
  `status` TINYINT NOT NULL DEFAULT 1,
  `seller_reply` VARCHAR(500) DEFAULT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY `idx_product` (`product_id`),
  KEY `idx_seller` (`seller_id`),
  KEY `idx_buyer` (`buyer_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `report` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `reporter_id` BIGINT NOT NULL,
  `product_id` BIGINT NOT NULL,
  `report_type` TINYINT NOT NULL DEFAULT 1,
  `reason` VARCHAR(500) DEFAULT '',
  `images` VARCHAR(2000) DEFAULT '',
  `status` TINYINT NOT NULL DEFAULT 1,
  `handled_by` BIGINT DEFAULT NULL,
  `handle_remark` VARCHAR(500) DEFAULT '',
  `handled_at` DATETIME DEFAULT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY `idx_product` (`product_id`),
  KEY `idx_reporter` (`reporter_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `product_appeal` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `product_id` BIGINT NOT NULL,
  `seller_id` BIGINT NOT NULL,
  `appeal_reason` VARCHAR(1000) DEFAULT '',
  `images` VARCHAR(2000) DEFAULT '',
  `status` TINYINT NOT NULL DEFAULT 1,
  `appeal_count` TINYINT NOT NULL DEFAULT 1,
  `handled_by` BIGINT DEFAULT NULL,
  `reply` VARCHAR(500) DEFAULT '',
  `handled_at` DATETIME DEFAULT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY `idx_product` (`product_id`),
  KEY `idx_seller` (`seller_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `admin_log` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `operator_id` BIGINT DEFAULT NULL,
  `operator_role` TINYINT DEFAULT 0,
  `action` VARCHAR(50) NOT NULL,
  `target_type` VARCHAR(20) NOT NULL,
  `target_id` BIGINT DEFAULT NULL,
  `detail` VARCHAR(1000) DEFAULT '',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY `idx_operator` (`operator_id`),
  KEY `idx_target` (`target_type`, `target_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ==================== 新增表 3 张 ====================

CREATE TABLE IF NOT EXISTS `announcement` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `title` VARCHAR(200) NOT NULL,
  `content` TEXT NOT NULL,
  `type` TINYINT NOT NULL DEFAULT 1,
  `status` TINYINT NOT NULL DEFAULT 1,
  `pinned` TINYINT NOT NULL DEFAULT 0,
  `is_force` TINYINT NOT NULL DEFAULT 0,
  `min_seconds` INT NOT NULL DEFAULT 0,
  `scroll` TINYINT NOT NULL DEFAULT 1,
  `show_on_publish` TINYINT NOT NULL DEFAULT 0,
  `created_by` BIGINT DEFAULT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY `idx_status_type` (`status`, `type`),
  KEY `idx_force` (`status`, `is_force`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `nickname_audit` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL,
  `old_nickname` VARCHAR(50) DEFAULT NULL,
  `new_nickname` VARCHAR(50) NOT NULL,
  `status` TINYINT NOT NULL DEFAULT 0,
  `reason` VARCHAR(500) DEFAULT NULL,
  `handled_by` BIGINT DEFAULT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `handled_at` DATETIME DEFAULT NULL,
  KEY `idx_user` (`user_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `wallet_log` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL,
  `type` VARCHAR(20) NOT NULL,
  `amount` DECIMAL(10,2) NOT NULL,
  `balance_before` DECIMAL(10,2) NOT NULL,
  `balance_after` DECIMAL(10,2) NOT NULL,
  `order_id` BIGINT DEFAULT NULL,
  `remark` VARCHAR(200) DEFAULT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY `idx_user` (`user_id`),
  KEY `idx_order` (`order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

SET FOREIGN_KEY_CHECKS = 1;

-- ===== 字段注释补丁 =====
ALTER TABLE `admin_log` MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT 'ID';
ALTER TABLE `admin_log` MODIFY `operator_id` bigint(20) NULL COMMENT '操作人ID';
ALTER TABLE `admin_log` MODIFY `operator_role` tinyint(4) NULL DEFAULT '0' COMMENT '操作人角色';
ALTER TABLE `admin_log` MODIFY `action` varchar(50) NOT NULL COMMENT '操作类型';
ALTER TABLE `admin_log` MODIFY `target_type` varchar(20) NOT NULL COMMENT '目标类型';
ALTER TABLE `admin_log` MODIFY `target_id` bigint(20) NULL COMMENT '目标ID';
ALTER TABLE `admin_log` MODIFY `detail` varchar(1000) NULL DEFAULT '' COMMENT '详情';
ALTER TABLE `admin_log` MODIFY `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间';
ALTER TABLE `announcement` MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT 'ID';
ALTER TABLE `announcement` MODIFY `title` varchar(200) NOT NULL COMMENT '标题';
ALTER TABLE `announcement` MODIFY `content` text NOT NULL COMMENT '内容';
ALTER TABLE `announcement` MODIFY `type` tinyint(4) NOT NULL DEFAULT '1' COMMENT '类型 1公告2公约3通知';
ALTER TABLE `announcement` MODIFY `status` tinyint(4) NOT NULL DEFAULT '1' COMMENT '状态 1已发布0已下架';
ALTER TABLE `announcement` MODIFY `pinned` tinyint(4) NOT NULL DEFAULT '0' COMMENT '是否置顶';
ALTER TABLE `announcement` MODIFY `is_force` tinyint(4) NOT NULL DEFAULT '0' COMMENT '是否强制弹窗';
ALTER TABLE `announcement` MODIFY `min_seconds` int(11) NOT NULL DEFAULT '0' COMMENT '最低停留秒';
ALTER TABLE `announcement` MODIFY `scroll` tinyint(4) NOT NULL DEFAULT '1' COMMENT '是否滚动展示';
ALTER TABLE `announcement` MODIFY `show_on_publish` tinyint(4) NOT NULL DEFAULT '0' COMMENT '发布页展示';
ALTER TABLE `announcement` MODIFY `created_by` bigint(20) NULL COMMENT '发布人ID';
ALTER TABLE `announcement` MODIFY `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发布时间';
ALTER TABLE `announcement` MODIFY `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间';
ALTER TABLE `banner` MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT 'ID';
ALTER TABLE `banner` MODIFY `title` varchar(100) NULL COMMENT '标题';
ALTER TABLE `banner` MODIFY `image_url` varchar(500) NOT NULL COMMENT '图片地址';
ALTER TABLE `banner` MODIFY `link_url` varchar(500) NULL COMMENT '跳转链接';
ALTER TABLE `banner` MODIFY `sort_order` int(11) NULL DEFAULT '0' COMMENT '排序';
ALTER TABLE `banner` MODIFY `status` tinyint(4) NULL DEFAULT '1' COMMENT '状态 1启用0停用';
ALTER TABLE `banner` MODIFY `created_at` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间';
ALTER TABLE `banner` MODIFY `updated_at` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间';
ALTER TABLE `category` MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT 'ID';
ALTER TABLE `category` MODIFY `name` varchar(50) NOT NULL COMMENT '分类名';
ALTER TABLE `category` MODIFY `parent_id` bigint(20) NULL DEFAULT '0' COMMENT '父分类ID';
ALTER TABLE `category` MODIFY `level` tinyint(4) NULL DEFAULT '1' COMMENT '层级 1一级2二级';
ALTER TABLE `category` MODIFY `sort_order` int(11) NULL DEFAULT '0' COMMENT '排序';
ALTER TABLE `category` MODIFY `icon_url` varchar(500) NULL COMMENT '图标';
ALTER TABLE `category` MODIFY `created_at` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间';
ALTER TABLE `conversation` MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT 'ID';
ALTER TABLE `conversation` MODIFY `user1_id` bigint(20) NOT NULL COMMENT '用户1ID';
ALTER TABLE `conversation` MODIFY `user2_id` bigint(20) NOT NULL COMMENT '用户2ID';
ALTER TABLE `conversation` MODIFY `product_id` bigint(20) NULL COMMENT '关联商品ID';
ALTER TABLE `conversation` MODIFY `last_message` varchar(500) NULL COMMENT '最后一条消息';
ALTER TABLE `conversation` MODIFY `last_message_time` datetime NULL COMMENT '最后消息时间';
ALTER TABLE `conversation` MODIFY `user1_unread` int(11) NULL DEFAULT '0' COMMENT '用户1未读数';
ALTER TABLE `conversation` MODIFY `user2_unread` int(11) NULL DEFAULT '0' COMMENT '用户2未读数';
ALTER TABLE `conversation` MODIFY `created_at` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间';
ALTER TABLE `conversation` MODIFY `updated_at` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间';
ALTER TABLE `favorite` MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT 'ID';
ALTER TABLE `favorite` MODIFY `user_id` bigint(20) NOT NULL COMMENT '用户ID';
ALTER TABLE `favorite` MODIFY `product_id` bigint(20) NOT NULL COMMENT '商品ID';
ALTER TABLE `favorite` MODIFY `created_at` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '收藏时间';
ALTER TABLE `message` MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT 'ID';
ALTER TABLE `message` MODIFY `conversation_id` bigint(20) NOT NULL COMMENT '会话ID';
ALTER TABLE `message` MODIFY `sender_id` bigint(20) NOT NULL COMMENT '发送者';
ALTER TABLE `message` MODIFY `receiver_id` bigint(20) NOT NULL COMMENT '接收者';
ALTER TABLE `message` MODIFY `content` text NOT NULL COMMENT '消息内容';
ALTER TABLE `message` MODIFY `message_type` tinyint(4) NULL DEFAULT '1' COMMENT '类型 1文字2图片3系统';
ALTER TABLE `message` MODIFY `extra` json NULL COMMENT '额外数据JSON';
ALTER TABLE `message` MODIFY `is_read` tinyint(4) NULL DEFAULT '0' COMMENT '是否已读';
ALTER TABLE `message` MODIFY `created_at` datetime(3) NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '发送时间';
ALTER TABLE `nickname_audit` MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT 'ID';
ALTER TABLE `nickname_audit` MODIFY `user_id` bigint(20) NOT NULL COMMENT '用户ID';
ALTER TABLE `nickname_audit` MODIFY `old_nickname` varchar(50) NULL COMMENT '原昵称';
ALTER TABLE `nickname_audit` MODIFY `new_nickname` varchar(50) NOT NULL COMMENT '新昵称';
ALTER TABLE `nickname_audit` MODIFY `status` tinyint(4) NOT NULL DEFAULT '0' COMMENT '状态 0待审核1通过2拒绝';
ALTER TABLE `nickname_audit` MODIFY `reason` varchar(500) NULL COMMENT '处理说明';
ALTER TABLE `nickname_audit` MODIFY `handled_by` bigint(20) NULL COMMENT '处理人ID';
ALTER TABLE `nickname_audit` MODIFY `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '申请时间';
ALTER TABLE `nickname_audit` MODIFY `handled_at` datetime NULL COMMENT '处理时间';
ALTER TABLE `order` MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT 'ID';
ALTER TABLE `order` MODIFY `order_no` varchar(32) NOT NULL COMMENT '订单号';
ALTER TABLE `order` MODIFY `buyer_id` bigint(20) NOT NULL COMMENT '买家ID';
ALTER TABLE `order` MODIFY `seller_id` bigint(20) NOT NULL COMMENT '卖家ID';
ALTER TABLE `order` MODIFY `product_id` bigint(20) NOT NULL COMMENT '商品ID';
ALTER TABLE `order` MODIFY `product_title` varchar(200) NOT NULL COMMENT '商品标题(快照)';
ALTER TABLE `order` MODIFY `product_image` varchar(500) NULL COMMENT '商品封面(快照)';
ALTER TABLE `order` MODIFY `price` decimal(10,2) NOT NULL COMMENT '商品价格';
ALTER TABLE `order` MODIFY `freight_amount` decimal(10,2) NULL DEFAULT '0.00' COMMENT '运费';
ALTER TABLE `order` MODIFY `total_amount` decimal(10,2) NOT NULL COMMENT '订单总额';
ALTER TABLE `order` MODIFY `address_id` bigint(20) NOT NULL COMMENT '地址ID';
ALTER TABLE `order` MODIFY `address_snapshot` json NULL COMMENT '地址快照JSON';
ALTER TABLE `order` MODIFY `status` tinyint(4) NOT NULL DEFAULT '0' COMMENT '状态 0待支付1已支付2已发货3已收货4已完成5已取消';
ALTER TABLE `order` MODIFY `payment_method` varchar(20) NULL COMMENT '支付方式';
ALTER TABLE `order` MODIFY `escrow` decimal(10,2) NULL COMMENT '平台托管金';
ALTER TABLE `order` MODIFY `payment_time` datetime NULL COMMENT '支付时间';
ALTER TABLE `order` MODIFY `ship_company` varchar(50) NULL COMMENT '快递公司';
ALTER TABLE `order` MODIFY `ship_no` varchar(50) NULL COMMENT '快递单号';
ALTER TABLE `order` MODIFY `ship_time` datetime NULL COMMENT '发货时间';
ALTER TABLE `order` MODIFY `receive_time` datetime NULL COMMENT '收货时间';
ALTER TABLE `order` MODIFY `complete_time` datetime NULL COMMENT '完成时间';
ALTER TABLE `order` MODIFY `cancel_time` datetime NULL COMMENT '取消时间';
ALTER TABLE `order` MODIFY `cancel_reason` varchar(500) NULL COMMENT '取消原因';
ALTER TABLE `order` MODIFY `buyer_rated` tinyint(4) NULL DEFAULT '0' COMMENT '买家已评价';
ALTER TABLE `order` MODIFY `seller_rated` tinyint(4) NULL DEFAULT '0' COMMENT '卖家已评价';
ALTER TABLE `order` MODIFY `created_at` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间';
ALTER TABLE `order` MODIFY `updated_at` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间';
ALTER TABLE `product` MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT 'ID';
ALTER TABLE `product` MODIFY `seller_id` bigint(20) NOT NULL COMMENT '卖家ID';
ALTER TABLE `product` MODIFY `category_id` bigint(20) NOT NULL COMMENT '分类ID';
ALTER TABLE `product` MODIFY `title` varchar(200) NOT NULL COMMENT '标题';
ALTER TABLE `product` MODIFY `description` text NULL COMMENT '描述';
ALTER TABLE `product` MODIFY `price` decimal(10,2) NOT NULL COMMENT '售价';
ALTER TABLE `product` MODIFY `original_price` decimal(10,2) NULL COMMENT '原价';
ALTER TABLE `product` MODIFY `condition` tinyint(4) NOT NULL COMMENT '成色 1全新2几乎全新3轻微使用4明显使用';
ALTER TABLE `product` MODIFY `status` tinyint(4) NULL DEFAULT '1' COMMENT '状态 1在售2下架3违规4已售';
ALTER TABLE `product` MODIFY `review_reason` varchar(500) NULL COMMENT '违规原因';
ALTER TABLE `product` MODIFY `freight_type` tinyint(4) NULL DEFAULT '1' COMMENT '运费类型 1包邮2买家承担';
ALTER TABLE `product` MODIFY `freight_amount` decimal(10,2) NULL DEFAULT '0.00' COMMENT '运费';
ALTER TABLE `product` MODIFY `view_count` int(11) NULL DEFAULT '0' COMMENT '浏览量';
ALTER TABLE `product` MODIFY `favorite_count` int(11) NULL DEFAULT '0' COMMENT '收藏数';
ALTER TABLE `product` MODIFY `location` varchar(100) NULL COMMENT '所在地';
ALTER TABLE `product` MODIFY `created_at` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间';
ALTER TABLE `product` MODIFY `updated_at` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间';
ALTER TABLE `product_appeal` MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT 'ID';
ALTER TABLE `product_appeal` MODIFY `product_id` bigint(20) NOT NULL COMMENT '商品ID';
ALTER TABLE `product_appeal` MODIFY `seller_id` bigint(20) NOT NULL COMMENT '卖家ID';
ALTER TABLE `product_appeal` MODIFY `appeal_reason` varchar(1000) NULL DEFAULT '' COMMENT '申诉理由';
ALTER TABLE `product_appeal` MODIFY `images` varchar(2000) NULL DEFAULT '' COMMENT '图片';
ALTER TABLE `product_appeal` MODIFY `status` tinyint(4) NOT NULL DEFAULT '1' COMMENT '状态 1待审核2通过3驳回';
ALTER TABLE `product_appeal` MODIFY `appeal_count` tinyint(4) NOT NULL DEFAULT '1' COMMENT '申诉次数';
ALTER TABLE `product_appeal` MODIFY `handled_by` bigint(20) NULL COMMENT '处理人';
ALTER TABLE `product_appeal` MODIFY `reply` varchar(500) NULL DEFAULT '' COMMENT '处理回复';
ALTER TABLE `product_appeal` MODIFY `handled_at` datetime NULL COMMENT '处理时间';
ALTER TABLE `product_appeal` MODIFY `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间';
ALTER TABLE `product_appeal` MODIFY `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间';
ALTER TABLE `product_comment` MODIFY `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT COMMENT 'ID';
ALTER TABLE `product_comment` MODIFY `product_id` bigint(20) NOT NULL COMMENT '商品ID';
ALTER TABLE `product_comment` MODIFY `user_id` bigint(20) NOT NULL COMMENT '用户ID';
ALTER TABLE `product_comment` MODIFY `parent_id` bigint(20) NOT NULL DEFAULT '0' COMMENT '父评论ID';
ALTER TABLE `product_comment` MODIFY `content` varchar(1000) NOT NULL COMMENT '内容';
ALTER TABLE `product_comment` MODIFY `status` tinyint(4) NOT NULL DEFAULT '1' COMMENT '状态 1正常0删除';
ALTER TABLE `product_comment` MODIFY `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间';
ALTER TABLE `product_image` MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT 'ID';
ALTER TABLE `product_image` MODIFY `product_id` bigint(20) NOT NULL COMMENT '商品ID';
ALTER TABLE `product_image` MODIFY `url` varchar(500) NOT NULL COMMENT '图片地址';
ALTER TABLE `product_image` MODIFY `sort_order` int(11) NULL DEFAULT '0' COMMENT '排序';
ALTER TABLE `product_image` MODIFY `is_cover` tinyint(4) NULL DEFAULT '0' COMMENT '是否封面 1是0否';
ALTER TABLE `product_image` MODIFY `created_at` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间';
ALTER TABLE `product_intent` MODIFY `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT COMMENT 'ID';
ALTER TABLE `product_intent` MODIFY `product_id` bigint(20) NOT NULL COMMENT '商品ID';
ALTER TABLE `product_intent` MODIFY `seller_id` bigint(20) NOT NULL COMMENT '卖家ID';
ALTER TABLE `product_intent` MODIFY `buyer_id` bigint(20) NOT NULL COMMENT '买家ID';
ALTER TABLE `product_intent` MODIFY `message` varchar(500) NULL COMMENT '留言';
ALTER TABLE `product_intent` MODIFY `expected_price` decimal(10,2) NULL COMMENT '期望价';
ALTER TABLE `product_intent` MODIFY `status` tinyint(4) NOT NULL DEFAULT '1' COMMENT '状态 1待回复2已回复3已成交4已关闭';
ALTER TABLE `product_intent` MODIFY `seller_reply` varchar(500) NULL COMMENT '卖家回复';
ALTER TABLE `product_intent` MODIFY `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间';
ALTER TABLE `product_intent` MODIFY `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间';
ALTER TABLE `rating` MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT 'ID';
ALTER TABLE `rating` MODIFY `order_id` bigint(20) NOT NULL COMMENT '订单ID';
ALTER TABLE `rating` MODIFY `rater_id` bigint(20) NOT NULL COMMENT '评价人';
ALTER TABLE `rating` MODIFY `rated_user_id` bigint(20) NOT NULL COMMENT '被评价人';
ALTER TABLE `rating` MODIFY `role` tinyint(4) NOT NULL COMMENT '角色 1买家2卖家';
ALTER TABLE `rating` MODIFY `score` tinyint(4) NOT NULL COMMENT '评分 1~5';
ALTER TABLE `rating` MODIFY `comment` varchar(500) NULL COMMENT '评价内容';
ALTER TABLE `rating` MODIFY `tags` json NULL COMMENT '标签JSON';
ALTER TABLE `rating` MODIFY `created_at` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '评价时间';
ALTER TABLE `report` MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT 'ID';
ALTER TABLE `report` MODIFY `reporter_id` bigint(20) NOT NULL COMMENT '举报人';
ALTER TABLE `report` MODIFY `product_id` bigint(20) NOT NULL COMMENT '商品ID';
ALTER TABLE `report` MODIFY `report_type` tinyint(4) NOT NULL DEFAULT '1' COMMENT '类型 1违规2虚假3其他';
ALTER TABLE `report` MODIFY `reason` varchar(500) NULL DEFAULT '' COMMENT '举报原因';
ALTER TABLE `report` MODIFY `images` varchar(2000) NULL DEFAULT '' COMMENT '图片';
ALTER TABLE `report` MODIFY `status` tinyint(4) NOT NULL DEFAULT '1' COMMENT '状态 1待处理2已处理3驳回';
ALTER TABLE `report` MODIFY `handled_by` bigint(20) NULL COMMENT '处理人';
ALTER TABLE `report` MODIFY `handle_remark` varchar(500) NULL DEFAULT '' COMMENT '处理备注';
ALTER TABLE `report` MODIFY `handled_at` datetime NULL COMMENT '处理时间';
ALTER TABLE `report` MODIFY `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间';
ALTER TABLE `report` MODIFY `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间';
ALTER TABLE `user` MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '用户ID';
ALTER TABLE `user` MODIFY `username` varchar(100) NOT NULL COMMENT '用户名(登录账号,唯一)';
ALTER TABLE `user` MODIFY `password` varchar(255) NOT NULL COMMENT '密码(加密存储)';
ALTER TABLE `user` MODIFY `phone` varchar(20) NULL COMMENT '手机号';
ALTER TABLE `user` MODIFY `email` varchar(100) NULL COMMENT '邮箱';
ALTER TABLE `user` MODIFY `openid` varchar(64) NULL COMMENT '微信openid';
ALTER TABLE `user` MODIFY `email_verified` tinyint(4) NULL DEFAULT '0' COMMENT '邮箱是否验证';
ALTER TABLE `user` MODIFY `avatar_url` varchar(500) NULL COMMENT '头像地址';
ALTER TABLE `user` MODIFY `nickname` varchar(50) NULL COMMENT '昵称';
ALTER TABLE `user` MODIFY `nickname_pending` varchar(50) NULL COMMENT '待审核昵称';
ALTER TABLE `user` MODIFY `nickname_status` tinyint(4) NOT NULL DEFAULT '0' COMMENT '昵称状态 0正常1审核中';
ALTER TABLE `user` MODIFY `bio` text NULL COMMENT '个人简介';
ALTER TABLE `user` MODIFY `gender` tinyint(4) NULL DEFAULT '0' COMMENT '性别 0保密1男2女';
ALTER TABLE `user` MODIFY `status` tinyint(4) NULL DEFAULT '1' COMMENT '状态 1正常0封禁';
ALTER TABLE `user` MODIFY `role` tinyint(4) NULL DEFAULT '0' COMMENT '角色 0普通1管理员2审核';
ALTER TABLE `user` MODIFY `login_source` varchar(20) NULL DEFAULT 'email' COMMENT '注册来源 浏览email/wechat/phone';
ALTER TABLE `user` MODIFY `reputation_score` decimal(3,1) NULL DEFAULT '5.0' COMMENT '信誉分 0~5';
ALTER TABLE `user` MODIFY `balance` decimal(10,2) NOT NULL DEFAULT '0.00' COMMENT '账户余额';
ALTER TABLE `user` MODIFY `last_login_at` datetime NULL COMMENT '最近登录';
ALTER TABLE `user` MODIFY `created_at` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '注册时间';
ALTER TABLE `user` MODIFY `updated_at` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间';
ALTER TABLE `user_address` MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT 'ID';
ALTER TABLE `user_address` MODIFY `user_id` bigint(20) NOT NULL COMMENT '所属用户';
ALTER TABLE `user_address` MODIFY `receiver_name` varchar(50) NOT NULL COMMENT '收货人';
ALTER TABLE `user_address` MODIFY `phone` varchar(20) NOT NULL COMMENT '收货电话';
ALTER TABLE `user_address` MODIFY `province` varchar(50) NULL COMMENT '省';
ALTER TABLE `user_address` MODIFY `city` varchar(50) NULL COMMENT '市';
ALTER TABLE `user_address` MODIFY `district` varchar(50) NULL COMMENT '区县';
ALTER TABLE `user_address` MODIFY `detail` varchar(255) NOT NULL COMMENT '详细地址';
ALTER TABLE `user_address` MODIFY `postal_code` varchar(10) NULL COMMENT '邮编';
ALTER TABLE `user_address` MODIFY `is_default` tinyint(4) NULL DEFAULT '0' COMMENT '是否默认地址 1是0否';
ALTER TABLE `user_address` MODIFY `created_at` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间';
ALTER TABLE `user_address` MODIFY `updated_at` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间';
ALTER TABLE `wallet_log` MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT 'ID';
ALTER TABLE `wallet_log` MODIFY `user_id` bigint(20) NOT NULL COMMENT '用户ID';
ALTER TABLE `wallet_log` MODIFY `type` varchar(20) NOT NULL COMMENT '类型 recharge/pay/refund/receive';
ALTER TABLE `wallet_log` MODIFY `amount` decimal(10,2) NOT NULL COMMENT '金额(正收入负支出)';
ALTER TABLE `wallet_log` MODIFY `balance_before` decimal(10,2) NOT NULL COMMENT '变动前余额';
ALTER TABLE `wallet_log` MODIFY `balance_after` decimal(10,2) NOT NULL COMMENT '变动后余额';
ALTER TABLE `wallet_log` MODIFY `order_id` bigint(20) NULL COMMENT '关联订单ID';
ALTER TABLE `wallet_log` MODIFY `remark` varchar(200) NULL COMMENT '备注';
ALTER TABLE `wallet_log` MODIFY `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间';