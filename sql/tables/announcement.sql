-- =============================================================
-- 新增表：announcement 平台公告表
-- 说明：管理员发布公告/平台公约/通知，用户端公告页与首页横幅展示。
-- 用法：mysql -uroot -p c2c < tables/announcement.sql
--       （CREATE TABLE IF NOT EXISTS，重复执行安全）
-- =============================================================
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

-- =============================================================
-- 演示数据（可选执行）：表为空时插入 3 条示例公告，幂等安全
-- =============================================================
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
