-- =============================================================
-- 压测专用批量造号脚本（JMeter 配套）
-- 用途：为压测批量生成一批普通买家账号，配合 PerfTokenGeneratorTest 签压测 token。
-- 适用：本地 / 测试库；切勿在生产库执行。
-- 口径：role=0、status=1（即 token 生成器筛选的普通买家），与现有演示账号同一密码，
--       明文 ww123456，BCrypt 哈希直接复用 data.sql 现有值（已校验 matches 通过）。
-- 幂等：可重复执行——先清理本号段旧压测号，再重新插入；不触碰管理员/审核员/助手等其他账号。
-- 调整数量：改末尾递归 CTE 的上限（当前 50，对应手机号 15000000001 ~ 15000000050）。
-- 执行：mysql -uroot -p c2c < JMeter/seed-perf-users.sql
-- 之后：mvn -o test -Dtest=PerfTokenGeneratorTest 重新生成 JMeter/result/perf-tokens.csv
-- =============================================================

SET NAMES utf8mb4;
USE c2c;

-- 复用现有演示账号的 BCrypt 哈希（明文均为 ww123456）
SET @perf_pwd := '$2a$10$jBqTdo1bQwxKPxppHH6Ot.Aqr3YHtBNOpx.BloyqFRKkHoIZXQOZS';

-- 清理上一批压测号（手机号段 15000000%，仅普通用户）
DELETE FROM `user` WHERE `phone` LIKE '15000000%' AND `role` = 0;

-- 递归生成 50 个连续手机号：15000000001 ~ 15000000050
INSERT INTO `user`
(`username`, `password`, `phone`, `email`, `email_verified`, `login_source`,
 `avatar_url`, `nickname`, `bio`, `gender`, `status`, `role`,
 `admin_level`, `admin_perms`, `token_version`, `reputation_score`, `balance`)
WITH RECURSIVE seq AS (
    SELECT 1 AS n
    UNION ALL
    SELECT n + 1 FROM seq WHERE n < 50
)
SELECT
    CONCAT('1500000', LPAD(seq.n, 4, '0')),
    @perf_pwd,
    CONCAT('1500000', LPAD(seq.n, 4, '0')),
    NULL,
    0,
    'phone',
    NULL,
    CONCAT('压测用户', LPAD(seq.n, 3, '0')),
    '压测专用账号，可随时清理',
    0,
    1,
    0,
    0,
    NULL,
    0,
    5.0,
    0.00
FROM seq;
