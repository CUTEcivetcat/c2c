-- =====================================================
-- C2C 迁移脚本 2026-08-10（纯静态版，宝塔/命令行均可执行）
-- 功能：商品审核（违规下架原因） + 商品评论 + 购买意向（我想要/询价砍价）
--
-- 用法一（命令行，服务器上）：
--   mysql -u root -p c2c --force < migration-20260810.sql
--   密码见 config/ 外部配置；--force 让"列已存在"的报错跳过、继续建表
--
-- 用法二（宝塔面板）：数据库 -> c2c -> 管理 -> SQL 标签。
--   建议逐条执行下面三条；若提示 Duplicate column 说明列已存在，忽略即可。
-- =====================================================

-- -----------------------------------------------------
-- 1. 商品表：违规下架原因（列已存在时执行会报 Duplicate column，可忽略）
-- -----------------------------------------------------
ALTER TABLE product
    ADD COLUMN review_reason VARCHAR(500) DEFAULT NULL COMMENT '违规下架原因（管理员填写）' AFTER `status`;

-- -----------------------------------------------------
-- 2. 商品评论表
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS product_comment (
    id         BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    product_id BIGINT NOT NULL COMMENT '商品ID',
    user_id    BIGINT NOT NULL COMMENT '评论用户ID',
    parent_id  BIGINT NOT NULL DEFAULT 0 COMMENT '父评论ID，0=一级评论',
    content    VARCHAR(1000) NOT NULL COMMENT '评论内容',
    status     TINYINT NOT NULL DEFAULT 1 COMMENT '1正常 0已删除',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    KEY idx_product (product_id, status),
    KEY idx_parent (parent_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品评论';

-- -----------------------------------------------------
-- 3. 购买意向表（我想要/询价砍价）
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS product_intent (
    id             BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    product_id     BIGINT NOT NULL COMMENT '商品ID',
    seller_id      BIGINT NOT NULL COMMENT '卖家ID（冗余，便于卖家查询）',
    buyer_id       BIGINT NOT NULL COMMENT '意向买家ID',
    message        VARCHAR(500) DEFAULT NULL COMMENT '买家留言/询价',
    expected_price DECIMAL(10,2) DEFAULT NULL COMMENT '买家期望价格',
    status         TINYINT NOT NULL DEFAULT 1 COMMENT '1待处理 2已回复 3已成交 4已关闭',
    seller_reply   VARCHAR(500) DEFAULT NULL COMMENT '卖家回复',
    created_at     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    KEY idx_product (product_id),
    KEY idx_seller (seller_id),
    KEY idx_buyer (buyer_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品购买意向（我想要）';
