-- =============================================================
-- C2C 全量建表脚本（init.sql）
-- 包含 18 张表：15 基础表 + announcement + nickname_audit + wallet_log
-- 用法：mysql -uroot -p c2c < init.sql   （会清空重建 c2c 库）
--       再执行 data.sql 导入演示数据
-- =============================================================
SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;
DROP DATABASE IF EXISTS c2c;
CREATE DATABASE c2c DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE c2c;

-- ==================== 基础表 15 张 ====================

-- user 用户表
CREATE TABLE user (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  username VARCHAR(100) NOT NULL,
  password VARCHAR(255) NOT NULL,
  phone VARCHAR(20) UNIQUE,
  email VARCHAR(100),
  email_verified TINYINT DEFAULT 0,
  vatar_url VARCHAR(500),
  
ickname VARCHAR(50),
  
ickname_pending VARCHAR(50) DEFAULT NULL,
  
ickname_status TINYINT NOT NULL DEFAULT 0,
  io TEXT,
  gender TINYINT DEFAULT 0,
  status TINYINT DEFAULT 1,
  ole TINYINT DEFAULT 0,
  eputation_score DECIMAL(3,1) DEFAULT 5.0,
  alance DECIMAL(10,2) NOT NULL DEFAULT 0.00,
  last_login_at DATETIME,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_phone (phone),
  INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- user_address 收货地址表
CREATE TABLE user_address (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  eceiver_name VARCHAR(50) NOT NULL,
  phone VARCHAR(20) NOT NULL,
  province VARCHAR(50),
  city VARCHAR(50),
  district VARCHAR(50),
  detail VARCHAR(255) NOT NULL,
  postal_code VARCHAR(10),
  is_default TINYINT DEFAULT 0,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- category 商品分类表
CREATE TABLE category (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  
ame VARCHAR(50) NOT NULL,
  parent_id BIGINT DEFAULT 0,
  level TINYINT DEFAULT 1,
  sort_order INT DEFAULT 0,
  icon_url VARCHAR(500),
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_parent_id (parent_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- product 商品表
CREATE TABLE product (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  seller_id BIGINT NOT NULL,
  category_id BIGINT NOT NULL,
  	itle VARCHAR(200) NOT NULL,
  description TEXT,
  price DECIMAL(10,2) NOT NULL,
  original_price DECIMAL(10,2),
  condition TINYINT NOT NULL,
  status TINYINT DEFAULT 1,
  eview_reason VARCHAR(500) DEFAULT NULL,
  reight_type TINYINT DEFAULT 1,
  reight_amount DECIMAL(10,2) DEFAULT 0,
  iew_count INT DEFAULT 0,
  avorite_count INT DEFAULT 0,
  location VARCHAR(100),
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_seller_id (seller_id),
  INDEX idx_category_status (category_id, status),
  INDEX idx_price (price),
  INDEX idx_view_count (iew_count),
  FULLTEXT INDEX t_search (	itle, description)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- product_image 商品图片表
CREATE TABLE product_image (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  product_id BIGINT NOT NULL,
  url VARCHAR(500) NOT NULL,
  sort_order INT DEFAULT 0,
  is_cover TINYINT DEFAULT 0,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_product_id (product_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- order 订单表
CREATE TABLE order (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  order_no VARCHAR(32) NOT NULL UNIQUE,
  uyer_id BIGINT NOT NULL,
  seller_id BIGINT NOT NULL,
  product_id BIGINT NOT NULL,
  product_title VARCHAR(200) NOT NULL,
  product_image VARCHAR(500),
  price DECIMAL(10,2) NOT NULL,
  reight_amount DECIMAL(10,2) DEFAULT 0,
  	otal_amount DECIMAL(10,2) NOT NULL,
  ddress_id BIGINT NOT NULL,
  ddress_snapshot JSON,
  status TINYINT NOT NULL DEFAULT 0,
  payment_method VARCHAR(20),
  escrow DECIMAL(10,2) DEFAULT NULL,
  payment_time DATETIME,
  ship_company VARCHAR(50),
  ship_no VARCHAR(50),
  ship_time DATETIME,
  eceive_time DATETIME,
  complete_time DATETIME,
  cancel_time DATETIME,
  cancel_reason VARCHAR(500),
  uyer_rated TINYINT DEFAULT 0,
  seller_rated TINYINT DEFAULT 0,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_order_no (order_no),
  INDEX idx_buyer_status (uyer_id, status),
  INDEX idx_seller_status (seller_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- favorite 收藏表
CREATE TABLE avorite (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  product_id BIGINT NOT NULL,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  UNIQUE INDEX uk_user_product (user_id, product_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- rating 评价表
CREATE TABLE ating (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  order_id BIGINT NOT NULL,
  ater_id BIGINT NOT NULL,
  ated_user_id BIGINT NOT NULL,
  ole TINYINT NOT NULL,
  score TINYINT NOT NULL,
  comment VARCHAR(500),
  	ags JSON,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  UNIQUE INDEX uk_order_rater (order_id, ater_id),
  INDEX idx_rated_user (ated_user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- conversation 会话表
CREATE TABLE conversation (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user1_id BIGINT NOT NULL,
  user2_id BIGINT NOT NULL,
  product_id BIGINT,
  last_message VARCHAR(500),
  last_message_time DATETIME,
  user1_unread INT DEFAULT 0,
  user2_unread INT DEFAULT 0,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE INDEX uk_user_product (user1_id, user2_id, product_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- message 消息表
CREATE TABLE message (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  conversation_id BIGINT NOT NULL,
  sender_id BIGINT NOT NULL,
  eceiver_id BIGINT NOT NULL,
  content TEXT NOT NULL,
  message_type TINYINT DEFAULT 1,
  extra JSON,
  is_read TINYINT DEFAULT 0,
  created_at DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3),
  INDEX idx_conversation (conversation_id, created_at),
  INDEX idx_receiver_unread (eceiver_id, is_read)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- product_comment 商品评论表
CREATE TABLE product_comment (
  id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  product_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  parent_id BIGINT NOT NULL DEFAULT 0,
  content VARCHAR(1000) NOT NULL,
  status TINYINT NOT NULL DEFAULT 1,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_product (product_id, status),
  KEY idx_parent (parent_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- product_intent 购买意向表
CREATE TABLE product_intent (
  id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  product_id BIGINT NOT NULL,
  seller_id BIGINT NOT NULL,
  uyer_id BIGINT NOT NULL,
  message VARCHAR(500) DEFAULT NULL,
  expected_price DECIMAL(10,2) DEFAULT NULL,
  status TINYINT NOT NULL DEFAULT 1,
  seller_reply VARCHAR(500) DEFAULT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY idx_product (product_id),
  KEY idx_seller (seller_id),
  KEY idx_buyer (uyer_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- report 举报表
CREATE TABLE eport (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  eporter_id BIGINT NOT NULL,
  product_id BIGINT NOT NULL,
  eport_type TINYINT NOT NULL DEFAULT 1,
  eason VARCHAR(500) DEFAULT '',
  images VARCHAR(2000) DEFAULT '',
  status TINYINT NOT NULL DEFAULT 1,
  handled_by BIGINT DEFAULT NULL,
  handle_remark VARCHAR(500) DEFAULT '',
  handled_at DATETIME DEFAULT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY idx_product (product_id),
  KEY idx_reporter (eporter_id),
  KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- product_appeal 整改申诉表
CREATE TABLE product_appeal (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  product_id BIGINT NOT NULL,
  seller_id BIGINT NOT NULL,
  ppeal_reason VARCHAR(1000) DEFAULT '',
  images VARCHAR(2000) DEFAULT '',
  status TINYINT NOT NULL DEFAULT 1,
  ppeal_count TINYINT NOT NULL DEFAULT 1,
  handled_by BIGINT DEFAULT NULL,
  eply VARCHAR(500) DEFAULT '',
  handled_at DATETIME DEFAULT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY idx_product (product_id),
  KEY idx_seller (seller_id),
  KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- admin_log 管理/审核操作日志表
CREATE TABLE dmin_log (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  operator_id BIGINT DEFAULT NULL,
  operator_role TINYINT DEFAULT 0,
  ction VARCHAR(50) NOT NULL,
  	arget_type VARCHAR(20) NOT NULL,
  	arget_id BIGINT DEFAULT NULL,
  detail VARCHAR(1000) DEFAULT '',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_operator (operator_id),
  KEY idx_target (	arget_type, 	arget_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ==================== 新增表 ====================

-- announcement 平台公告表
CREATE TABLE IF NOT EXISTS nnouncement (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  	itle VARCHAR(200) NOT NULL,
  content TEXT NOT NULL,
  	ype TINYINT NOT NULL DEFAULT 1,
  status TINYINT NOT NULL DEFAULT 1,
  pinned TINYINT NOT NULL DEFAULT 0,
  is_force TINYINT NOT NULL DEFAULT 0,
  min_seconds INT NOT NULL DEFAULT 0,
  scroll TINYINT NOT NULL DEFAULT 1,
  show_on_publish TINYINT NOT NULL DEFAULT 0,
  created_by BIGINT DEFAULT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY idx_status_type (status, 	ype),
  KEY idx_force (status, is_force)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- nickname_audit 昵称审核表
CREATE TABLE IF NOT EXISTS 
ickname_audit (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  old_nickname VARCHAR(50) DEFAULT NULL,
  
ew_nickname VARCHAR(50) NOT NULL,
  status TINYINT NOT NULL DEFAULT 0,
  eason VARCHAR(500) DEFAULT NULL,
  handled_by BIGINT DEFAULT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  handled_at DATETIME DEFAULT NULL,
  KEY idx_user (user_id),
  KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- wallet_log 资金流水表
CREATE TABLE IF NOT EXISTS wallet_log (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  	ype VARCHAR(20) NOT NULL,
  mount DECIMAL(10,2) NOT NULL,
  alance_before DECIMAL(10,2) NOT NULL,
  alance_after DECIMAL(10,2) NOT NULL,
  order_id BIGINT DEFAULT NULL,
  emark VARCHAR(200) DEFAULT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_user (user_id),
  KEY idx_order (order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


ALTER TABLE user ADD COLUMN balance DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '账户余额' AFTER reputation_score;
ALTER TABLE order ADD COLUMN escrow DECIMAL(10,2) DEFAULT NULL COMMENT '平台托管金额' AFTER payment_method;
SET FOREIGN_KEY_CHECKS = 1;
-- =============================================================
-- 建表完成：18 张表
-- =============================================================