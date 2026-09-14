-- =============================================================
-- 管理端三级权限体系（总管理员 / 次级·子级管理员 / 审核员）
--
-- 1) user 表新增：
--    - admin_level  TINYINT  管理员等级：0 非管理员 / 1 次级·子级管理员 / 2 总管理员(母管理)
--    - admin_perms  VARCHAR  管理员权限码（逗号分隔）；admin_level=2 视为全部权限，可存 '*'
-- 2) 权限码（模块级）：
--    dashboard 数据大屏 / users 用户管理 / products 商品管理 / orders 订单管理
--    review 审核管理 / announcement 公告管理 / banner 轮播图管理
--    permissions 权限管理（仅总管理员）/ security 安全设置
-- 3) 审核员（role=2）是最低级别：仅能进入用户端「审核工作台」，不能登录管理端口（/admin/login 仅接受 role=1）
--
-- 用法：mysql -uroot -p c2c < update-20260915-admin-levels.sql
-- =============================================================
SET NAMES utf8mb4;
USE c2c;

ALTER TABLE `user`
  ADD COLUMN `admin_level` TINYINT NOT NULL DEFAULT 0 COMMENT '管理员等级 0非管理员 1次级·子级管理员 2总管理员admin' AFTER `role`,
  ADD COLUMN `admin_perms` VARCHAR(500) NULL COMMENT '管理员权限码(逗号分隔)；admin_level=2 视为全部权限' AFTER `admin_level`;

-- 存量迁移：username='admin' 为总管理员（最高权限）；其余 role=1 管理员按次级管理员处理
UPDATE `user` SET `admin_level` = 2, `admin_perms` = '*' WHERE `role` = 1 AND `username` = 'admin';
UPDATE `user` SET `admin_level` = 1 WHERE `role` = 1 AND `username` <> 'admin' AND (`admin_level` IS NULL OR `admin_level` = 0);
