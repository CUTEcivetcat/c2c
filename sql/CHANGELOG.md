# 数据库变更记录（CHANGELOG）

> 本文档记录**每次表结构变更**及对应可执行 SQL，供手动部署数据库使用。
> 所有 SQL 幂等友好：列/表已存在时可能报 `Duplicate column`，**忽略即可**。

---

## 2026-08-21 · 公告增强（强制弹窗 + 滚动显示）

**变更内容**：`announcement` 表新增 3 列 + 1 索引，用于"登录强制弹窗、最低停留秒数、首页横幅滚动"。

**执行 SQL**（在 c2c 库执行；列已存在则跳过）：

```sql
-- 1. 是否强制弹窗（登录时弹出）
ALTER TABLE `announcement`
  ADD COLUMN `is_force` TINYINT NOT NULL DEFAULT 0
  COMMENT '是否强制弹窗（登录时弹出）：1是 0否' AFTER `pinned`;

-- 2. 最低停留秒数（is_force=1 时生效，期间不可关闭）
ALTER TABLE `announcement`
  ADD COLUMN `min_seconds` INT NOT NULL DEFAULT 0
  COMMENT '强制弹窗最低停留秒数（is_force=1时生效）' AFTER `is_force`;

-- 3. 是否滚动显示（首页横幅轮播）
ALTER TABLE `announcement`
  ADD COLUMN `scroll` TINYINT NOT NULL DEFAULT 1
  COMMENT '首页横幅是否滚动显示：1滚动 0不滚动' AFTER `min_seconds`;

-- 4. 强制公告查询索引
ALTER TABLE `announcement` ADD INDEX `idx_force` (`status`, `is_force`);
```

**演示数据**（可选）：

```sql
-- 欢迎公告设为强制弹窗（停留 5 秒）+ 滚动
UPDATE `announcement` SET is_force = 1, min_seconds = 5, scroll = 1 WHERE title = '欢迎使用闲小鱼';
-- 其余公告参与滚动
UPDATE `announcement` SET scroll = 1 WHERE scroll IS NULL OR scroll = 0;
```

> 已部署环境若用 `sql/rebuild-full.sql` 重建，则无需上述变更（脚本已含新结构）。

---

## 更早变更（已并入基础表/新增表文件）

| 日期 | 变更 | 位置 |
|---|---|---|
| 2026-08-21 | 昵称审核：nickname_audit 表 + user 加列（nickname_pending/nickname_status） | `sql/tables/nickname_audit.sql` |
| 2026-08-21 | 平台公告：announcement 表 | `sql/tables/announcement.sql` |
| 历史 | 评论/意向/举报/申诉/日志等表 | 已并入 `sql/schema.sql`（15 张基础表） |
