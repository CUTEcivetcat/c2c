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