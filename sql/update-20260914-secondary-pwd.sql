-- =============================================================
-- 管理端二级密码（高危操作二次校验）
-- 1) user 表新增 secondary_pwd：删除账户等高风险操作需输入二级密码
-- 2) 现有 role=1 的管理员中，username='admin' 为超级管理员（可配置次级管理员）
--    其余 role=1 且 username<>'admin' 的账户均为次级管理员
-- 用法：mysql -uroot -p c2c < update-20260914-secondary-pwd.sql
-- =============================================================
SET NAMES utf8mb4;
USE c2c;

ALTER TABLE `user`
  ADD COLUMN `secondary_pwd` VARCHAR(255) NULL COMMENT '二级密码(BCrypt加密)，删除账户等高危操作校验' AFTER `role`;
