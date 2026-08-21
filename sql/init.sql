-- =============================================================
-- C2C 全量建表脚本（init.sql）
-- 包含 17 张表：15 基础表 + announcement + nickname_audit + wallet_log 及 ALTER
-- 用法：mysql -uroot -p c2c < init.sql
--       再执行 data.sql 导入演示数据
-- 注意：会清空重建 c2c 库！
-- =============================================================
SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;
DROP DATABASE IF EXISTS c2c;
CREATE DATABASE c2c DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE c2c;

-- ==================== 基础表（15 张，来自 schema.sql） ====================
CREATE TABLE `user` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `username` VARCHAR(100) NOT NULL COMMENT '用户名（登录账号，唯一业务约束）',
  `password` VARCHAR(255) NOT NULL COMMENT '密码（BCrypt 加密）',
  `phone` VARCHAR(20) UNIQUE COMMENT '手机号',
  `email` VARCHAR(100) COMMENT '邮箱',
  `email_verified` TINYINT DEFAULT 0 COMMENT '邮箱是否已验证 0=否 1=是',
  `avatar_url` VARCHAR(500) COMMENT '头像地址',
  `nickname` VARCHAR(50) COMMENT '昵称',
  `bio` TEXT COMMENT '个人简介',
  `gender` TINYINT DEFAULT 0 COMMENT '性别 0=保密 1=男 2=女',
  `status` TINYINT DEFAULT 1 COMMENT '状态 1=正常 0=封禁',
  `role` TINYINT DEFAULT 0 COMMENT '角色 0=普通用户 1=管理员 2=审核员',
  `reputation_score` DECIMAL(3,1) DEFAULT 5.0 COMMENT '信誉评分（0.0~10.0）',
  `last_login_at` DATETIME COMMENT '最近登录时间',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '注册时间',
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  INDEX `idx_phone` (`phone`),
  INDEX `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表：平台账号，role=1 为管理员，role=2 为审核员';

-- =============================================================
-- 2. user_address 收货地址表
--    用户在发布/下单时填写的收货地址。is_default 标记默认地址。
-- =============================================================
CREATE TABLE `user_address` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL COMMENT '所属用户 ID',
  `receiver_name` VARCHAR(50) NOT NULL COMMENT '收货人姓名',
  `phone` VARCHAR(20) NOT NULL COMMENT '收货人电话',
  `province` VARCHAR(50) COMMENT '省',
  `city` VARCHAR(50) COMMENT '市',
  `district` VARCHAR(50) COMMENT '区/县',
  `detail` VARCHAR(255) NOT NULL COMMENT '详细地址',
  `postal_code` VARCHAR(10) COMMENT '邮编',
  `is_default` TINYINT DEFAULT 0 COMMENT '是否默认地址 1=是 0=否',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='收货地址表：用户的收货地址簿';

-- =============================================================
-- 3. category 商品分类表
--    两级分类树。parent_id=0 为一级分类，否则指向父分类。
--    演示数据名称已中文化，前端分类导航/发布下拉直接展示。
-- =============================================================
CREATE TABLE `category` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `name` VARCHAR(50) NOT NULL COMMENT '分类名称（中文）',
  `parent_id` BIGINT DEFAULT 0 COMMENT '父分类 ID，0=一级分类',
  `level` TINYINT DEFAULT 1 COMMENT '层级 1=一级 2=二级',
  `sort_order` INT DEFAULT 0 COMMENT '排序权重，越小越靠前',
  `icon_url` VARCHAR(500) COMMENT '分类图标',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  INDEX `idx_parent_id` (`parent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品分类表：两级分类树';

-- =============================================================
-- 4. product 商品表
--    二手商品的发布信息。status 贯穿整个交易生命周期：
--    1=在售 2=已预定 3=已售 4=下架 5=违规下架。
--    review_reason 记录违规下架原因（审核员/管理员填写）。
-- =============================================================
CREATE TABLE `product` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `seller_id` BIGINT NOT NULL COMMENT '卖家用户 ID',
  `category_id` BIGINT NOT NULL COMMENT '分类 ID（关联 category）',
  `title` VARCHAR(200) NOT NULL COMMENT '商品标题',
  `description` TEXT COMMENT '商品描述',
  `price` DECIMAL(10,2) NOT NULL COMMENT '售价',
  `original_price` DECIMAL(10,2) COMMENT '原价/参考价',
  `condition` TINYINT NOT NULL COMMENT '成色 1=全新 2=几乎全新 3=轻微使用 4=明显使用',
  `status` TINYINT DEFAULT 1 COMMENT '状态 1=在售 2=已预定 3=已售 4=下架 5=违规下架',
  `review_reason` VARCHAR(500) DEFAULT NULL COMMENT '违规下架原因（管理员填写）',
  `freight_type` TINYINT DEFAULT 1 COMMENT '运费方式 1=包邮 2=买家承担',
  `freight_amount` DECIMAL(10,2) DEFAULT 0 COMMENT '运费金额',
  `view_count` INT DEFAULT 0 COMMENT '浏览次数',
  `favorite_count` INT DEFAULT 0 COMMENT '收藏次数',
  `location` VARCHAR(100) COMMENT '交易地点',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '发布时间',
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX `idx_seller_id` (`seller_id`),
  INDEX `idx_category_status` (`category_id`, `status`),
  INDEX `idx_price` (`price`),
  INDEX `idx_view_count` (`view_count`),
  FULLTEXT INDEX `ft_search` (`title`, `description`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品表：二手商品发布信息';

-- =============================================================
-- 5. product_image 商品图片表
--    一个商品多张图，is_cover=1 为主图。url 存相对路径（/files/...），
--    由 Nginx 映射到本地上传目录。
-- =============================================================
CREATE TABLE `product_image` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `product_id` BIGINT NOT NULL COMMENT '商品 ID',
  `url` VARCHAR(500) NOT NULL COMMENT '图片地址（/files/...）',
  `sort_order` INT DEFAULT 0 COMMENT '排序权重',
  `is_cover` TINYINT DEFAULT 0 COMMENT '是否主图 1=是',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  INDEX `idx_product_id` (`product_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品图片表：商品的图片列表';

-- =============================================================
-- 6. order 订单表
--    交易订单。下单时把商品标题/图片/地址快照冗余到订单里，
--    避免商品或地址被修改/删除后订单信息丢失。
--    status：0=待支付 1=已支付 2=已发货 3=已收货 4=已完成 5=已取消
-- =============================================================
CREATE TABLE `order` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `order_no` VARCHAR(32) NOT NULL UNIQUE COMMENT '订单号（唯一）',
  `buyer_id` BIGINT NOT NULL COMMENT '买家用户 ID',
  `seller_id` BIGINT NOT NULL COMMENT '卖家用户 ID',
  `product_id` BIGINT NOT NULL COMMENT '商品 ID',
  `product_title` VARCHAR(200) NOT NULL COMMENT '商品标题快照',
  `product_image` VARCHAR(500) COMMENT '商品主图快照',
  `price` DECIMAL(10,2) NOT NULL COMMENT '商品单价',
  `freight_amount` DECIMAL(10,2) DEFAULT 0 COMMENT '运费',
  `total_amount` DECIMAL(10,2) NOT NULL COMMENT '订单总额',
  `address_id` BIGINT NOT NULL COMMENT '收货地址 ID',
  `address_snapshot` JSON COMMENT '收货地址快照（JSON）',
  `status` TINYINT NOT NULL DEFAULT 0 COMMENT '状态 0=待支付 1=已支付 2=已发货 3=已收货 4=已完成 5=已取消',
  `payment_method` VARCHAR(20) COMMENT '支付方式',
  `payment_time` DATETIME COMMENT '支付时间',
  `ship_company` VARCHAR(50) COMMENT '物流公司',
  `ship_no` VARCHAR(50) COMMENT '物流单号',
  `ship_time` DATETIME COMMENT '发货时间',
  `receive_time` DATETIME COMMENT '收货时间',
  `complete_time` DATETIME COMMENT '完成时间',
  `cancel_time` DATETIME COMMENT '取消时间',
  `cancel_reason` VARCHAR(500) COMMENT '取消原因',
  `buyer_rated` TINYINT DEFAULT 0 COMMENT '买家是否已评价 1=是',
  `seller_rated` TINYINT DEFAULT 0 COMMENT '卖家是否已评价 1=是',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '下单时间',
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX `idx_order_no` (`order_no`),
  INDEX `idx_buyer_status` (`buyer_id`, `status`),
  INDEX `idx_seller_status` (`seller_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单表：交易订单及状态流转';

-- =============================================================
-- 7. favorite 收藏表
--    用户收藏商品。uk_user_product 保证同一用户对同一商品只收藏一次。
-- =============================================================
CREATE TABLE `favorite` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL COMMENT '用户 ID',
  `product_id` BIGINT NOT NULL COMMENT '商品 ID',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  UNIQUE INDEX `uk_user_product` (`user_id`, `product_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='收藏表：用户收藏的商品';

-- =============================================================
-- 8. rating 评价表
--    订单完成后买卖双方互评。role 表示评价身份（1=买家 2=卖家），
--    score 为评分，uk_order_rater 保证每单每方只能评一次。
-- =============================================================
CREATE TABLE `rating` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `order_id` BIGINT NOT NULL COMMENT '订单 ID',
  `rater_id` BIGINT NOT NULL COMMENT '评价人 ID',
  `rated_user_id` BIGINT NOT NULL COMMENT '被评价人 ID',
  `role` TINYINT NOT NULL COMMENT '评价身份 1=买家 2=卖家',
  `score` TINYINT NOT NULL COMMENT '评分（1~5）',
  `comment` VARCHAR(500) COMMENT '评语',
  `tags` JSON COMMENT '评价标签（JSON 数组）',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  UNIQUE INDEX `uk_order_rater` (`order_id`, `rater_id`),
  INDEX `idx_rated_user` (`rated_user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='评价表：订单买卖双方互评';

-- =============================================================
-- 9. conversation 会话表
--    买卖双方围绕某件商品的聊天会话（一个商品一个会话）。
--    unread 字段分别记录两端未读数。
-- =============================================================
CREATE TABLE `conversation` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `user1_id` BIGINT NOT NULL COMMENT '会话发起方用户 ID',
  `user2_id` BIGINT NOT NULL COMMENT '会话接收方用户 ID',
  `product_id` BIGINT COMMENT '关联商品 ID（可为空=通用会话）',
  `last_message` VARCHAR(500) COMMENT '最后一条消息内容',
  `last_message_time` DATETIME COMMENT '最后一条消息时间',
  `user1_unread` INT DEFAULT 0 COMMENT 'user1 未读数',
  `user2_unread` INT DEFAULT 0 COMMENT 'user2 未读数',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE INDEX `uk_user_product` (`user1_id`, `user2_id`, `product_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='会话表：买卖双方聊天会话';

-- =============================================================
-- 10. message 消息表
--     会话内的具体消息，按时间排序。message_type 区分文本/图片/系统通知。
-- =============================================================
CREATE TABLE `message` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `conversation_id` BIGINT NOT NULL COMMENT '所属会话 ID',
  `sender_id` BIGINT NOT NULL COMMENT '发送方用户 ID',
  `receiver_id` BIGINT NOT NULL COMMENT '接收方用户 ID',
  `content` TEXT NOT NULL COMMENT '消息内容',
  `message_type` TINYINT DEFAULT 1 COMMENT '消息类型 1=文本 2=图片 4=系统通知',
  `extra` JSON COMMENT '扩展信息',
  `is_read` TINYINT DEFAULT 0 COMMENT '是否已读 1=是',
  `created_at` DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3) COMMENT '发送时间（毫秒精度）',
  INDEX `idx_conversation` (`conversation_id`, `created_at`),
  INDEX `idx_receiver_unread` (`receiver_id`, `is_read`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='消息表：会话内的聊天消息';

-- =============================================================
-- 11. product_comment 商品评论表
--     买家/游客可查看商品评论，登录用户可发表/回复/删除。
-- =============================================================
CREATE TABLE `product_comment` (
  `id` BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  `product_id` BIGINT NOT NULL COMMENT '商品ID',
  `user_id` BIGINT NOT NULL COMMENT '评论用户ID',
  `parent_id` BIGINT NOT NULL DEFAULT 0 COMMENT '父评论ID，0=一级评论',
  `content` VARCHAR(1000) NOT NULL COMMENT '评论内容',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '1正常 0已删除',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  KEY `idx_product` (`product_id`, `status`),
  KEY `idx_parent` (`parent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品评论';

-- =============================================================
-- 12. product_intent 购买意向表（我想要/询价砍价）
--     买家发起购买意向，卖家可回复/成交/关闭。
-- =============================================================
CREATE TABLE `product_intent` (
  `id` BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  `product_id` BIGINT NOT NULL COMMENT '商品ID',
  `seller_id` BIGINT NOT NULL COMMENT '卖家ID（冗余，便于卖家查询）',
  `buyer_id` BIGINT NOT NULL COMMENT '意向买家ID',
  `message` VARCHAR(500) DEFAULT NULL COMMENT '买家留言/询价',
  `expected_price` DECIMAL(10,2) DEFAULT NULL COMMENT '买家期望价格',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '1待处理 2已回复 3已成交 4已关闭',
  `seller_reply` VARCHAR(500) DEFAULT NULL COMMENT '卖家回复',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  KEY `idx_product` (`product_id`),
  KEY `idx_seller` (`seller_id`),
  KEY `idx_buyer` (`buyer_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品购买意向（我想要）';

-- =============================================================
-- 13. report 商品举报表
--     任何登录用户可举报商品，审核员/管理员处理。
-- =============================================================
CREATE TABLE `report` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `reporter_id` BIGINT NOT NULL COMMENT '举报人用户ID',
  `product_id` BIGINT NOT NULL COMMENT '被举报商品ID',
  `report_type` TINYINT NOT NULL DEFAULT 1 COMMENT '举报类型：1违禁品 2假冒伪劣 3描述不符 4欺诈 5侵权 6其他',
  `reason` VARCHAR(500) DEFAULT '' COMMENT '举报理由',
  `images` VARCHAR(2000) DEFAULT '' COMMENT '举报附图URL，逗号分隔',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1待处理 2已违规下架 3已驳回',
  `handled_by` BIGINT DEFAULT NULL COMMENT '处理人用户ID（管理员/审核员）',
  `handle_remark` VARCHAR(500) DEFAULT '' COMMENT '处理备注/驳回理由',
  `handled_at` DATETIME DEFAULT NULL COMMENT '处理时间',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '举报时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  KEY `idx_product` (`product_id`),
  KEY `idx_reporter` (`reporter_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品举报';

-- =============================================================
-- 14. product_appeal 商品整改申诉表
--     违规下架后商户整改提交，审核员/管理员重新审核上架（最多 3 次）。
-- =============================================================
CREATE TABLE `product_appeal` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `product_id` BIGINT NOT NULL COMMENT '商品ID',
  `seller_id` BIGINT NOT NULL COMMENT '卖家用户ID',
  `appeal_reason` VARCHAR(1000) DEFAULT '' COMMENT '整改说明/申诉理由',
  `images` VARCHAR(2000) DEFAULT '' COMMENT '整改附图URL，逗号分隔',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1待审核 2已通过(恢复上架) 3已驳回',
  `appeal_count` TINYINT NOT NULL DEFAULT 1 COMMENT '第几次申诉（最多3次）',
  `handled_by` BIGINT DEFAULT NULL COMMENT '处理人用户ID（管理员/审核员）',
  `reply` VARCHAR(500) DEFAULT '' COMMENT '审核回复（驳回/通过说明）',
  `handled_at` DATETIME DEFAULT NULL COMMENT '处理时间',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '申诉时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  KEY `idx_product` (`product_id`),
  KEY `idx_seller` (`seller_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品整改申诉';

-- =============================================================
-- 15. admin_log 管理/审核操作日志表
--     下架、恢复、举报处理、申诉处理、角色分配留痕。
-- =============================================================
CREATE TABLE `admin_log` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `operator_id` BIGINT DEFAULT NULL COMMENT '操作人用户ID（管理员/审核员）',
  `operator_role` TINYINT DEFAULT 0 COMMENT '操作人角色：1管理员 2审核员',
  `action` VARCHAR(50) NOT NULL COMMENT '动作：ban/restore/report_handle/appeal_handle/set_role',
  `target_type` VARCHAR(20) NOT NULL COMMENT '对象类型：product/report/appeal/user',
  `target_id` BIGINT DEFAULT NULL COMMENT '对象ID',
  `detail` VARCHAR(1000) DEFAULT '' COMMENT '操作详情（含理由）',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
  KEY `idx_operator` (`operator_id`),
  KEY `idx_target` (`target_type`, `target_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='管理/审核操作日志';


-- =============================================================

-- =============================================================
-- 18. wallet_log 资金流水表
--     记录每笔余额变动：充值/支付/退款/到账，进出双向留痕。
-- =============================================================
-- ==================== 新增表（来自 tables/） ====================

-- announcement 平台公告表
CREATE TABLE IF NOT EXISTS `announcement` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `title` VARCHAR(200) NOT NULL COMMENT '公告标题',
  `content` TEXT NOT NULL COMMENT '公告内容',
  `type` TINYINT NOT NULL DEFAULT 1 COMMENT '类型：1公告 2平台公约 3通知',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1已发布 0已下架',
  `pinned` TINYINT NOT NULL DEFAULT 0 COMMENT '是否置顶：1置顶 0普通',
  `is_force` TINYINT NOT NULL DEFAULT 0 COMMENT '是否强制弹窗（登录时弹出）：1是 0否',
  `min_seconds` INT NOT NULL DEFAULT 0 COMMENT '强制弹窗最低停留秒数（is_force=1时生效）',
  `scroll` TINYINT NOT NULL DEFAULT 1 COMMENT '首页横幅是否滚动显示：1滚动 0不滚动',
  `show_on_publish` TINYINT NOT NULL DEFAULT 0 COMMENT '是否在发布商品页右侧展示：1展示 0不展示',
  `created_by` BIGINT DEFAULT NULL COMMENT '发布人用户ID',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发布时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  KEY `idx_status_type` (`status`, `type`),
  KEY `idx_force` (`status`, `is_force`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='平台公告';

-- nickname_audit 昵称审核表
CREATE TABLE IF NOT EXISTS `nickname_audit` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL COMMENT '申请人用户ID',
  `old_nickname` VARCHAR(50) DEFAULT NULL COMMENT '原昵称',
  `new_nickname` VARCHAR(50) NOT NULL COMMENT '申请的新昵称',
  `status` TINYINT NOT NULL DEFAULT 0 COMMENT '状态：0待审核 1已通过 2已拒绝',
  `reason` VARCHAR(500) DEFAULT NULL COMMENT '处理说明/拒绝原因',
  `handled_by` BIGINT DEFAULT NULL COMMENT '处理人用户ID（审核员/管理员）',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '申请时间',
  `handled_at` DATETIME DEFAULT NULL COMMENT '处理时间',
  KEY `idx_user` (`user_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='昵称修改审核记录';

ALTER TABLE `user` ADD COLUMN `nickname_pending` VARCHAR(50) DEFAULT NULL COMMENT '待审核昵称' AFTER `nickname`;
ALTER TABLE `user` ADD COLUMN `nickname_status` TINYINT NOT NULL DEFAULT 0 COMMENT '昵称状态：0正常 1审核中' AFTER `nickname_pending`;

-- wallet_log 资金流水表
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

ALTER TABLE `user` ADD COLUMN IF NOT EXISTS `balance` DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '账户余额' AFTER `reputation_score`;
ALTER TABLE `order` ADD COLUMN IF NOT EXISTS `escrow` DECIMAL(10,2) DEFAULT NULL COMMENT '平台托管金额（支付时暂扣，收货后打给卖家）' AFTER `payment_method`;

SET FOREIGN_KEY_CHECKS = 1;
-- =============================================================
-- 建表完成：17 张表
-- =============================================================
