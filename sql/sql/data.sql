-- =============================================================
-- C2C 全量演示数据（data.sql）
-- 在 init.sql 建表完成后执行
-- 用法：mysql -uroot -p c2c < data.sql
-- =============================================================
SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;
USE c2c;

-- ==================== 基础表演示数据（来自 demo-data.sql） ====================
-- =============================================================
-- C2C 二手校园交易平台 · 演示数据（demo-data.sql）
-- =============================================================
-- 说明：
--   1. 请在执行 schema.sql（建表）之后执行本脚本。
--   2. 密码统一为 Abc123456（BCrypt 加密），
--      包含 1 个管理员（admin/Abc123456，role=1）+ 3 个普通用户。
--   3. 商品图片 /files/demo/*.jpg 为演示占位路径。
--   4. 可重复执行（主键冲突会报错，属正常，不影响数据正确性）。
--
-- 导入方式（服务器上）：
--   mysql -uroot -p c2c < demo-data.sql
-- 或进入 MySQL 后：USE c2c; SOURCE demo-data.sql;
-- =============================================================

USE c2c;

-- -------------------------------------------------------------
-- 用户（id=4 为管理员，role=1）
--   13800000001 / Abc123456 小王（卖家）
--   13900000001 / Abc123456 小李（买家）
--   test@qq.com / Abc123456  测试用户
--   admin / Abc123456        管理员
-- -------------------------------------------------------------
INSERT INTO `user`
(`id`, `username`, `password`, `phone`, `email`, `email_verified`, `avatar_url`, `nickname`, `bio`, `gender`, `status`, `role`, `reputation_score`, `last_login_at`, `created_at`, `updated_at`)
VALUES
(1, '13800000001', '$2a$10$EsUcIYG8ER5jzxgnm27uGu./dInm5XHI8ze4xTTlfzy6sZOTUc9A2', '13800000001', NULL, 0, NULL, '小王', '学生卖家，出闲置', 1, 1, 0, 5.0, NOW(), NOW(), NOW()),
(2, '13900000001', '$2a$10$EsUcIYG8ER5jzxgnm27uGu./dInm5XHI8ze4xTTlfzy6sZOTUc9A2', '13900000001', NULL, 0, NULL, '小李', '学生买家，淘好物', 1, 1, 0, 4.8, NOW(), NOW(), NOW()),
(3, 'test@qq.com', '$2a$10$EsUcIYG8ER5jzxgnm27uGu./dInm5XHI8ze4xTTlfzy6sZOTUc9A2', NULL, 'test@qq.com', 1, NULL, '测试用户', '演示账号', 0, 1, 0, 5.0, NOW(), NOW(), NOW()),
(4, 'admin', '$2a$10$EsUcIYG8ER5jzxgnm27uGu./dInm5XHI8ze4xTTlfzy6sZOTUc9A2', '18800000001', NULL, 0, NULL, '管理员', '平台管理员', 1, 1, 1, 5.0, NOW(), NOW(), NOW());

-- -------------------------------------------------------------
-- 收货地址
-- -------------------------------------------------------------
INSERT INTO `user_address`
(`id`, `user_id`, `receiver_name`, `phone`, `province`, `city`, `district`, `detail`, `postal_code`, `is_default`, `created_at`, `updated_at`)
VALUES
(1, 1, '小王', '13800000001', '河南', '郑州', '高新区', '学生公寓A栋101', NULL, 1, NOW(), NOW()),
(2, 2, '小李', '13900000001', '河南', '郑州', '金水区', '学生公寓B栋506', NULL, 1, NOW(), NOW()),
(3, 3, '测试用户', '13900000001', '河南', '郑州', '高新区', '图书馆南门', NULL, 1, NOW(), NOW());

-- -------------------------------------------------------------
-- 商品分类（中文，两级）
-- -------------------------------------------------------------
INSERT INTO `category` (`id`, `name`, `parent_id`, `level`, `sort_order`, `created_at`) VALUES
(1, '书籍', 0, 1, 1, NOW()),
(2, '数码', 0, 1, 2, NOW()),
(3, '手机', 0, 1, 3, NOW()),
(4, '日用', 0, 1, 4, NOW()),
(101, '教材', 1, 2, 1, NOW()),
(102, '英语', 1, 2, 2, NOW()),
(201, '笔记本电脑', 2, 2, 1, NOW()),
(301, '苹果手机', 3, 2, 1, NOW()),
(401, '宿舍用品', 4, 2, 1, NOW());

-- -------------------------------------------------------------
-- 商品
-- -------------------------------------------------------------
INSERT INTO `product`
(`id`, `seller_id`, `category_id`, `title`, `description`, `price`, `original_price`, `condition`, `status`, `freight_type`, `freight_amount`, `view_count`, `favorite_count`, `location`, `created_at`, `updated_at`)
VALUES
(1, 1, 101, '高等数学教材', '期末复习用书，无笔记划痕，几乎全新', 25.00, 49.00, 2, 1, 1, 0, 128, 12, '南校区', NOW(), NOW()),
(2, 1, 201, 'MacBook Pro 2023', '16GB内存 512GB硬盘，成色很好', 8500.00, 12999.00, 2, 1, 1, 0, 320, 28, '北校区', NOW(), NOW()),
(3, 2, 301, 'iPhone 15 Pro 256G', '成色好，带原装盒', 5999.00, 8999.00, 2, 1, 1, 0, 560, 45, '主校区', NOW(), NOW()),
(4, 3, 401, '宿舍台灯', '用了两个月，功能正常', 45.00, 99.00, 2, 1, 1, 0, 45, 3, '宿舍', NOW(), NOW());

-- -------------------------------------------------------------
-- 商品图片（/files/demo/*.jpg 为演示占位路径）
-- -------------------------------------------------------------
INSERT INTO `product_image` (`id`, `product_id`, `url`, `sort_order`, `is_cover`, `created_at`) VALUES
(1, 1, '/files/demo/math.jpg', 0, 1, NOW()),
(2, 2, '/files/demo/macbook.jpg', 0, 1, NOW()),
(3, 3, '/files/demo/iphone.jpg', 0, 1, NOW()),
(4, 4, '/files/demo/lamp.jpg', 0, 1, NOW());

-- -------------------------------------------------------------
-- 订单（已完成：状态 4）
-- -------------------------------------------------------------
INSERT INTO `order`
(`id`, `order_no`, `buyer_id`, `seller_id`, `product_id`, `product_title`, `product_image`, `price`, `freight_amount`, `total_amount`, `address_id`, `address_snapshot`, `status`, `payment_method`, `payment_time`, `ship_company`, `ship_no`, `ship_time`, `receive_time`, `complete_time`, `cancel_time`, `cancel_reason`, `buyer_rated`, `seller_rated`, `created_at`, `updated_at`)
VALUES
(1, '20260615001', 2, 1, 1, '高等数学教材', '/files/demo/math.jpg', 25.00, 0, 25.00, 2, '{"receiverName":"小李","phone":"13900000001","detail":"学生公寓B栋506"}', 4, 'mock', '2026-06-15 10:30:00', '顺丰', 'SF123456789', '2026-06-15 14:00:00', '2026-06-17 09:00:00', '2026-06-17 09:00:00', NULL, NULL, 1, 1, '2026-06-15 10:00:00', NOW());

-- -------------------------------------------------------------
-- 收藏
-- -------------------------------------------------------------
INSERT INTO `favorite` (`id`, `user_id`, `product_id`, `created_at`) VALUES
(1, 1, 3, NOW()),
(2, 2, 2, NOW()),
(3, 3, 4, NOW());

-- -------------------------------------------------------------
-- 评价
-- -------------------------------------------------------------
INSERT INTO `rating` (`id`, `order_id`, `rater_id`, `rated_user_id`, `role`, `score`, `comment`, `tags`, `created_at`) VALUES
(1, 1, 2, 1, 1, 5, '发货快，书很新', '["发货快","书好"]', NOW());

-- -------------------------------------------------------------
-- 会话
-- -------------------------------------------------------------
INSERT INTO `conversation`
(`id`, `user1_id`, `user2_id`, `product_id`, `last_message`, `last_message_time`, `user1_unread`, `user2_unread`, `created_at`, `updated_at`)
VALUES
(1, 1, 2, 3, '还有货吗？', NOW(), 0, 1, NOW(), NOW());

-- -------------------------------------------------------------
-- 消息
-- -------------------------------------------------------------
INSERT INTO `message`
(`id`, `conversation_id`, `sender_id`, `receiver_id`, `content`, `message_type`, `extra`, `is_read`, `created_at`)
VALUES
(1, 1, 2, 1, '你好，iPhone 15 Pro 还有货吗？', 1, NULL, 1, NOW()),
(2, 1, 1, 2, '有的，可以在主校区当面看。', 1, NULL, 0, NOW());

-- ==================== 新增表演示数据 ====================

INSERT INTO `announcement` (`title`, `content`, `type`, `status`, `pinned`, `is_force`, `min_seconds`, `scroll`, `show_on_publish`, `created_by`)
SELECT tmp.title, tmp.content, tmp.type, tmp.status, tmp.pinned, tmp.is_force, tmp.min_seconds, tmp.scroll, tmp.show_on_publish, tmp.created_by FROM (
  SELECT '欢迎使用闲小鱼' AS title,
         '闲小鱼是一个 C2C 二手交易平台，让闲置流转起来。\n\n请先阅读《平台公约》，遵守交易规则，共同维护良好的交易环境。' AS content,
         1 AS type, 1 AS status, 1 AS pinned, 1 AS is_force, 5 AS min_seconds, 1 AS scroll, 1 AS show_on_publish, 4 AS created_by
  UNION ALL
  SELECT '平台公约',
         '1. 如实描述商品，禁止虚假信息与违规商品。\n2. 交易自愿，请勿线下脱离平台交易。\n3. 文明沟通，禁止辱骂、骚扰等不良行为。\n4. 违规商品将被下架，情节严重者封禁账号。',
         2, 1, 0, 0 AS is_force, 0 AS min_seconds, 1 AS scroll, 0 AS show_on_publish, 4 AS created_by
  UNION ALL
  SELECT '交易提示',
         '支付与收货请务必在平台内完成，谨防线下交易诈骗。',
         3, 1, 0, 0 AS is_force, 0 AS min_seconds, 1 AS scroll, 0 AS show_on_publish, 4 AS created_by
) tmp
WHERE NOT EXISTS (SELECT 1 FROM `announcement`);


INSERT INTO `nickname_audit` (`user_id`, `old_nickname`, `new_nickname`, `status`)
SELECT 1, '小王', '小闲鱼', 0
WHERE NOT EXISTS (SELECT 1 FROM `nickname_audit` WHERE `user_id` = 1 AND `status` = 0);


-- 演示用户各充值 100 元
UPDATE `user` SET balance = 100.00 WHERE id <= 8 AND balance = 0;

SET FOREIGN_KEY_CHECKS = 1;
-- =============================================================
-- 数据导入完成
-- =============================================================
