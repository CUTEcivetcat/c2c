-- =============================================================
-- 管理员等级修复（幂等，可反复执行；无 ALTER，不依赖迁移是否执行过）
--
-- 现象：admin 登录后左侧「权限管理」菜单消失 / 安全设置里无管理员管理卡片
-- 原因：user.admin_level 未迁移或值不正确（总管理员需 = 2）
--
-- 用法：mysql -uroot -p c2c < repair-admin-levels.sql
-- =============================================================
SET NAMES utf8mb4;
USE c2c;

-- 1) admin 账号恒为总管理员（最高权限）
UPDATE `user` SET `admin_level` = 2, `admin_perms` = '*' WHERE `role` = 1 AND `username` = 'admin';

-- 2) 存量管理员（role=1 且非 admin）默认转为次级管理员（权限由总管理员在「安全设置」中分配）
UPDATE `user` SET `admin_level` = 1 WHERE `role` = 1 AND `username` <> 'admin' AND (`admin_level` IS NULL OR `admin_level` = 0);

-- 3) 校验（应输出：admin_level=2 的记录 ≥ 1）
SELECT id, username, role, admin_level, admin_perms FROM `user` WHERE role = 1 ORDER BY id;
