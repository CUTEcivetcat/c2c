-- =============================================================
-- C2C 全量演示数据（data.sql）
-- 在 init.sql 建表完成后执行
-- 用法：mysql -uroot -p c2c < data.sql
-- =============================================================
SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;
USE c2c;

INSERT INTO `user`
(`id`, `username`, `password`, `phone`, `email`, `email_verified`, `avatar_url`, `nickname`, `bio`, `gender`, `status`, `role`, `admin_level`, `admin_perms`, `reputation_score`, `last_login_at`, `created_at`, `updated_at`)
VALUES
(1, '13800000001', '$2a$10$R2IByS.HRupurMLmZyJrBOd.NebEO3aNTD4f/Sy8GtsF/.UjRfe1W', '13800000001', NULL, 0, NULL, '小王', '学生卖家，出闲置', 1, 1, 0, 0, NULL, 5.0, NOW(), NOW(), NOW()),
(2, '13900000001', '$2a$10$R2IByS.HRupurMLmZyJrBOd.NebEO3aNTD4f/Sy8GtsF/.UjRfe1W', '13900000001', NULL, 0, NULL, '小李', '学生买家，淘好物', 1, 1, 0, 0, NULL, 4.8, NOW(), NOW(), NOW()),
(3, 'test@qq.com', '$2a$10$R2IByS.HRupurMLmZyJrBOd.NebEO3aNTD4f/Sy8GtsF/.UjRfe1W', NULL, 'test@qq.com', 1, NULL, '测试用户', '演示账号', 0, 1, 0, 0, NULL, 5.0, NOW(), NOW(), NOW()),
(4, 'admin', '$2a$10$R2IByS.HRupurMLmZyJrBOd.NebEO3aNTD4f/Sy8GtsF/.UjRfe1W', '18800000001', NULL, 0, NULL, '管理员', '平台管理员', 1, 1, 1, 2, '*', 5.0, NOW(), NOW(), NOW());

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

SET FOREIGN_KEY_CHECKS = 1;
-- =============================================================
-- 数据导入完成
-- =============================================================

-- =============================================================
-- 以下为扩充测试数据（与 data-extra.sql 一致；已建库时请单独执行 data-extra.sql）
-- =============================================================
SET FOREIGN_KEY_CHECKS = 0;
USE c2c;

-- -------------------------------------------------------------
-- 补充演示用户（id 5~8）
-- -------------------------------------------------------------
INSERT INTO `user`
(`id`, `username`, `password`, `phone`, `email`, `email_verified`, `avatar_url`, `nickname`, `bio`, `gender`, `status`, `role`, `admin_level`, `admin_perms`, `reputation_score`, `last_login_at`, `created_at`, `updated_at`)
VALUES
(5, '13700000001', '$2a$10$R2IByS.HRupurMLmZyJrBOd.NebEO3aNTD4f/Sy8GtsF/.UjRfe1W', '13700000001', NULL, 0, NULL, '张同学', '计算机学院，出考研复习资料', 1, 1, 0, 0, NULL, 5.0, NOW(), NOW(), NOW()),
(6, '13600000001', '$2a$10$R2IByS.HRupurMLmZyJrBOd.NebEO3aNTD4f/Sy8GtsF/.UjRfe1W', '13600000001', NULL, 0, NULL, '李学长', '毕业清仓，好价出数码', 1, 1, 0, 0, NULL, 4.9, NOW(), NOW(), NOW()),
(7, '13500000001', '$2a$10$R2IByS.HRupurMLmZyJrBOd.NebEO3aNTD4f/Sy8GtsF/.UjRfe1W', '13500000001', NULL, 0, NULL, '王学姐', '学姐闲置，物美价廉', 2, 1, 0, 0, NULL, 4.8, NOW(), NOW(), NOW()),
(8, '13400000001', '$2a$10$R2IByS.HRupurMLmZyJrBOd.NebEO3aNTD4f/Sy8GtsF/.UjRfe1W', '13400000001', NULL, 0, NULL, '赵师兄', '机电学院，宿舍神器低价出', 1, 1, 0, 0, NULL, 5.0, NOW(), NOW(), NOW());

-- -------------------------------------------------------------
-- 补充商品（id 5~30，覆盖教材/英语/笔记本电脑/苹果手机/宿舍用品全部分类）
-- -------------------------------------------------------------
INSERT INTO `product`
(`id`, `seller_id`, `category_id`, `title`, `description`, `price`, `original_price`, `condition`, `status`, `freight_type`, `freight_amount`, `view_count`, `favorite_count`, `location`, `created_at`, `updated_at`)
VALUES
-- 教材(101)
(5,  1, 101, '线性代数辅导讲义', '考研/期末复习用，笔记少，近全新', 15.00, 29.00, 2, 1, 1, 0, 88, 6, '南校区', NOW(), NOW()),
(6,  2, 101, 'C语言程序设计', '第四版教材，无折页无划痕', 18.00, 45.00, 1, 1, 1, 0, 156, 12, '主校区', NOW(), NOW()),
(7,  5, 101, '数据结构（C语言版）', '考研用书，附课后题答案，部分笔记', 20.00, 49.00, 2, 1, 1, 0, 202, 18, '北校区', NOW(), NOW()),
(8,  6, 101, '高等数学同济第七版', '上下册合售，有少量铅笔笔记', 16.00, 58.00, 3, 1, 1, 0, 74, 5, '南校区', NOW(), NOW()),
(9,  7, 101, '大学物理教材', '第二版，期末后闲置', 14.00, 52.00, 2, 1, 1, 0, 63, 4, '主校区', NOW(), NOW()),
(10, 8, 101, '机械制图习题集', '全新未写过', 12.00, 30.00, 1, 1, 1, 0, 45, 3, '北校区', NOW(), NOW()),
-- 英语(102)
(11, 5, 102, '新概念英语第二册', '正版，附配套练习册', 22.00, 55.00, 2, 1, 1, 0, 98, 7, '主校区', NOW(), NOW()),
(12, 6, 102, '考研英语词汇5500', '恋练有词，可扫码听音频', 19.00, 68.00, 2, 1, 1, 0, 167, 15, '南校区', NOW(), NOW()),
(13, 7, 102, '四级真题详解', '近五年真题+解析，字迹干净', 10.00, 35.00, 3, 1, 1, 0, 210, 22, '北校区', NOW(), NOW()),
(14, 8, 102, '雅思词汇红宝书', '出国备考，附自测本', 25.00, 60.00, 2, 1, 1, 0, 58, 4, '主校区', NOW(), NOW()),
-- 笔记本电脑(201)
(15, 1, 201, 'MacBook Air M2 8+256', '24年购入，无磕碰，电池循环40次，箱说全', 5200.00, 7999.00, 2, 1, 1, 0, 421, 38, '南校区', NOW(), NOW()),
(16, 2, 201, '联想小新Pro14', '16G+512G，办公学习够用，无维修史', 3200.00, 5699.00, 3, 1, 1, 0, 356, 30, '主校区', NOW(), NOW()),
(17, 5, 201, '华为MateBook 14', '2023款，2K触控屏，轻薄便携', 3800.00, 6099.00, 2, 1, 1, 0, 289, 24, '北校区', NOW(), NOW()),
(18, 6, 201, 'ThinkPad X1 Carbon', '商务轻薄本，指纹解锁，成色好', 4500.00, 12999.00, 3, 1, 1, 0, 178, 15, '南校区', NOW(), NOW()),
(19, 7, 201, '戴尔灵越15', '游戏办公两用，送无线鼠标', 2600.00, 4999.00, 4, 1, 1, 0, 132, 11, '宿舍', NOW(), NOW()),
-- 苹果手机(301)
(20, 2, 301, 'iPhone 14 128G', '国行双卡，电池健康89%，带壳带膜', 3499.00, 5999.00, 2, 1, 1, 0, 486, 42, '主校区', NOW(), NOW()),
(21, 5, 301, 'iPhone 13 128G', '蓝色，成色好，无暗病', 2599.00, 5999.00, 3, 1, 1, 0, 398, 33, '北校区', NOW(), NOW()),
(22, 6, 301, 'iPhone 12 64G', '自用一年半，功能正常，屏有细微划痕', 1999.00, 6299.00, 3, 1, 1, 0, 244, 19, '南校区', NOW(), NOW()),
(23, 7, 301, 'iPhone 15 256G', '2024年激活，全套配件，电池健康100%', 4899.00, 6999.00, 1, 1, 1, 0, 312, 27, '主校区', NOW(), NOW()),
-- 宿舍用品(401)
(24, 3, 401, '宿舍折叠桌', '床上小桌板，可调角度，用了一个月', 38.00, 89.00, 2, 1, 1, 0, 96, 8, '宿舍', NOW(), NOW()),
(25, 8, 401, '床帘三件套', '遮光款，宿舍单人床适用', 45.00, 129.00, 3, 1, 1, 0, 71, 6, '宿舍', NOW(), NOW()),
(26, 5, 401, 'USB小风扇', '三档风力，Type-C充电，静音', 15.00, 39.00, 2, 1, 1, 0, 88, 7, '宿舍', NOW(), NOW()),
(27, 6, 401, '桌面收纳盒', '三层抽屉式透明，收纳神器', 12.00, 29.00, 1, 1, 1, 0, 54, 4, '宿舍', NOW(), NOW()),
(28, 7, 401, '公牛插线板', '6位总控，带USB口，宿舍限电可用', 25.00, 59.00, 2, 1, 1, 0, 67, 5, '宿舍', NOW(), NOW()),
(29, 8, 401, '保温壶', '1.5L大容量，保温12小时', 30.00, 79.00, 3, 1, 1, 0, 49, 3, '宿舍', NOW(), NOW()),
(30, 3, 401, '宿舍小夜灯', '触控调光，USB供电，暖光', 9.00, 25.00, 1, 1, 1, 0, 103, 9, '宿舍', NOW(), NOW());

-- -------------------------------------------------------------
-- 补充商品图片（/files/demo/*.jpg 为演示占位路径）
-- -------------------------------------------------------------
INSERT INTO `product_image` (`id`, `product_id`, `url`, `sort_order`, `is_cover`, `created_at`) VALUES
(5, 5, '/files/demo/linear.jpg', 0, 1, NOW()),
(6, 6, '/files/demo/c.jpg', 0, 1, NOW()),
(7, 7, '/files/demo/ds.jpg', 0, 1, NOW()),
(8, 8, '/files/demo/math2.jpg', 0, 1, NOW()),
(9, 9, '/files/demo/physics.jpg', 0, 1, NOW()),
(10, 10, '/files/demo/drawing.jpg', 0, 1, NOW()),
(11, 11, '/files/demo/nce.jpg', 0, 1, NOW()),
(12, 12, '/files/demo/vocab.jpg', 0, 1, NOW()),
(13, 13, '/files/demo/cet4.jpg', 0, 1, NOW()),
(14, 14, '/files/demo/ielts.jpg', 0, 1, NOW()),
(15, 15, '/files/demo/mba.jpg', 0, 1, NOW()),
(16, 16, '/files/demo/xiaoxin.jpg', 0, 1, NOW()),
(17, 17, '/files/demo/matebook.jpg', 0, 1, NOW()),
(18, 18, '/files/demo/thinkpad.jpg', 0, 1, NOW()),
(19, 19, '/files/demo/dell.jpg', 0, 1, NOW()),
(20, 20, '/files/demo/iphone14.jpg', 0, 1, NOW()),
(21, 21, '/files/demo/iphone13.jpg', 0, 1, NOW()),
(22, 22, '/files/demo/iphone12.jpg', 0, 1, NOW()),
(23, 23, '/files/demo/iphone15.jpg', 0, 1, NOW()),
(24, 24, '/files/demo/table.jpg', 0, 1, NOW()),
(25, 25, '/files/demo/curtain.jpg', 0, 1, NOW()),
(26, 26, '/files/demo/fan.jpg', 0, 1, NOW()),
(27, 27, '/files/demo/box.jpg', 0, 1, NOW()),
(28, 28, '/files/demo/socket.jpg', 0, 1, NOW()),
(29, 29, '/files/demo/thermos.jpg', 0, 1, NOW()),
(30, 30, '/files/demo/nightlight.jpg', 0, 1, NOW());

-- -------------------------------------------------------------
-- 补充收藏
-- -------------------------------------------------------------
INSERT INTO `favorite` (`id`, `user_id`, `product_id`, `created_at`) VALUES
(4, 1, 15, NOW()),
(5, 2, 21, NOW()),
(6, 3, 24, NOW()),
(7, 5, 17, NOW()),
(8, 6, 20, NOW());

-- -------------------------------------------------------------
-- 补充购买意向（询价/砍价）
-- -------------------------------------------------------------
INSERT INTO `product_intent`
(`id`, `product_id`, `seller_id`, `buyer_id`, `message`, `expected_price`, `status`, `seller_reply`, `created_at`, `updated_at`)
VALUES
(1, 20, 2, 1, '3500能出吗？可以当面交易', 3400.00, 1, NULL, NOW(), NOW()),
(2, 15, 1, 2, '5200可以小刀一下吗？', 5000.00, 2, '最多5100，支持面交验机', NOW(), NOW()),
(3, 24, 3, 5, '折叠桌还在吗？', 35.00, 1, NULL, NOW(), NOW());

-- -------------------------------------------------------------
-- 首页轮播图（banner）
-- -------------------------------------------------------------
INSERT INTO `banner` (`id`, `title`, `image_url`, `link_url`, `sort_order`, `status`, `created_at`, `updated_at`) VALUES
(1, '开学季 · 数码好物专场', '/files/demo/iphone15.jpg', '/pages/product/detail?id=24', 1, 1, NOW(), NOW()),
(2, '宿舍收纳神器 · 二手价带走', '/files/demo/box.jpg', '/pages/product/detail?id=27', 2, 1, NOW(), NOW()),
(3, '考研资料一站备齐', '/files/demo/cet4.jpg', '/pages/product/detail?id=13', 3, 1, NOW(), NOW());

-- -------------------------------------------------------------
-- 公告（登录弹窗 + 滚动公告）
-- -------------------------------------------------------------
INSERT INTO `announcement` (`id`, `title`, `content`, `type`, `status`, `pinned`, `is_force`, `min_seconds`, `scroll`, `show_on_publish`, `created_by`, `created_at`, `updated_at`) VALUES
(1, '欢迎使用青禾集市', '亲爱的同学，欢迎来到青禾集市！\n\n在这里你可以：\n1. 发布闲置，让物品继续流转；\n2. 淘到物美价廉的二手好物；\n3. 站内私信沟通，安全交易。\n\n请遵守平台规则，诚信交易，共同维护绿色低碳的校园循环经济。', 1, 1, 1, 1, 3, 1, 0, 1, NOW(), NOW());

-- 全部用户（含扩充用户 5~8）各充值 100 元余额
UPDATE `user` SET `balance` = 100.00 WHERE `id` <= 8 AND `balance` = 0;
