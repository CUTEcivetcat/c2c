-- =====================================================
-- C2C 迁移脚本 2026-08-17（纯静态版，宝塔/命令行均可执行）
-- 功能：审核功能 —— 用户举报 + 商户整改申诉 + 审核操作日志（小法官模式）
-- 配套角色：user.role 新增 2=审核员（管理员在管理端分配，见权限管理页）
--
-- 用法一（命令行，服务器上）：
--   mysql -u root -p c2c --force < migration-20260817-review.sql
--   输入数据库密码；--force 让"表已存在"的报错跳过、继续
--
-- 用法二（宝塔面板）：数据库 -> c2c -> 管理 -> SQL 标签。
--   逐条执行下面三条；若提示 already exists 说明表已建，忽略即可。
-- =====================================================

-- -----------------------------------------------------
-- 1. 商品举报表：任何登录用户可举报商品，审核员/管理员处理
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS report (
    id            BIGINT PRIMARY KEY AUTO_INCREMENT,
    reporter_id   BIGINT NOT NULL COMMENT '举报人用户ID',
    product_id    BIGINT NOT NULL COMMENT '被举报商品ID',
    report_type   TINYINT NOT NULL DEFAULT 1 COMMENT '举报类型：1违禁品 2假冒伪劣 3描述不符 4欺诈 5侵权 6其他',
    reason        VARCHAR(500) DEFAULT '' COMMENT '举报理由',
    images        VARCHAR(2000) DEFAULT '' COMMENT '举报附图URL，逗号分隔',
    status        TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1待处理 2已违规下架 3已驳回',
    handled_by    BIGINT DEFAULT NULL COMMENT '处理人用户ID（管理员/审核员）',
    handle_remark VARCHAR(500) DEFAULT '' COMMENT '处理备注/驳回理由',
    handled_at    DATETIME DEFAULT NULL COMMENT '处理时间',
    created_at    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '举报时间',
    updated_at    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    KEY idx_product (product_id),
    KEY idx_reporter (reporter_id),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品举报';

-- -----------------------------------------------------
-- 2. 商品整改申诉表：违规下架后商户整改提交，审核员/管理员重新审核上架
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS product_appeal (
    id            BIGINT PRIMARY KEY AUTO_INCREMENT,
    product_id    BIGINT NOT NULL COMMENT '商品ID',
    seller_id     BIGINT NOT NULL COMMENT '卖家用户ID',
    appeal_reason VARCHAR(1000) DEFAULT '' COMMENT '整改说明/申诉理由',
    images        VARCHAR(2000) DEFAULT '' COMMENT '整改附图URL，逗号分隔',
    status        TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1待审核 2已通过(恢复上架) 3已驳回',
    appeal_count  TINYINT NOT NULL DEFAULT 1 COMMENT '第几次申诉（最多3次）',
    handled_by    BIGINT DEFAULT NULL COMMENT '处理人用户ID（管理员/审核员）',
    reply         VARCHAR(500) DEFAULT '' COMMENT '审核回复（驳回/通过说明）',
    handled_at    DATETIME DEFAULT NULL COMMENT '处理时间',
    created_at    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '申诉时间',
    updated_at    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    KEY idx_product (product_id),
    KEY idx_seller (seller_id),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品整改申诉';

-- -----------------------------------------------------
-- 3. 管理/审核操作日志表：下架、恢复、举报处理、申诉处理、角色分配留痕
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS admin_log (
    id            BIGINT PRIMARY KEY AUTO_INCREMENT,
    operator_id   BIGINT DEFAULT NULL COMMENT '操作人用户ID（管理员/审核员）',
    operator_role TINYINT DEFAULT 0 COMMENT '操作人角色：1管理员 2审核员',
    action        VARCHAR(50) NOT NULL COMMENT '动作：ban/restore/report_handle/appeal_handle/set_role',
    target_type   VARCHAR(20) NOT NULL COMMENT '对象类型：product/report/appeal/user',
    target_id     BIGINT DEFAULT NULL COMMENT '对象ID',
    detail        VARCHAR(1000) DEFAULT '' COMMENT '操作详情（含理由）',
    created_at    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
    KEY idx_operator (operator_id),
    KEY idx_target (target_type, target_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='管理/审核操作日志';
