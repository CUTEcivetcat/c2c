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

## 2026-08-21 · 发布页公告展示（show_on_publish）

**变更内容**：`announcement` 表新增 `show_on_publish` 列，用于控制公告是否在发布商品页右侧展示。

**执行 SQL**：

```sql
ALTER TABLE `announcement`
  ADD COLUMN `show_on_publish` TINYINT NOT NULL DEFAULT 0
  COMMENT '是否在发布商品页右侧展示：1展示 0不展示' AFTER `scroll`;
UPDATE `announcement` SET show_on_publish = 1 WHERE title = '欢迎使用闲小鱼';
```

---

## 2026-08-21 · 趋势数据改为真实查询

**变更内容**：管理端数据大屏的 7 日趋势折线图从**模拟数据**改为**真实数据库查询**（`user` 表的 `created_at`、`order` 表的 `created_at`/`total_amount`）。新增 `admin.mapper.DashboardMapper`。

---

## 2026-08-21 · 余额钱包 + 担保交易

**变更内容**：新增余额钱包体系，支付从模拟改为真实余额扣款。
- 新建 `wallet_log` 表（资金流水，充值/支付/退款/收款）
- `user` 表加 `balance` 列（余额）
- `order` 表加 `escrow` 列（平台托管金额）

**执行 SQL**（幂等，列已存在报错忽略）：

```sql
-- 1. 资金流水表
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

-- 2. user 表加余额
ALTER TABLE `user` ADD COLUMN `balance` DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '账户余额' AFTER `reputation_score`;

-- 3. order 表加托管金
ALTER TABLE `order` ADD COLUMN `escrow` DECIMAL(10,2) DEFAULT NULL COMMENT '平台托管金额（支付时暂扣，收货后打给卖家）' AFTER `payment_method`;

-- 4. 演示用户各充值 100 元
UPDATE `user` SET balance = 100.00 WHERE id <= 8 AND balance = 0;
```

---

## 更早变更（已并入基础表/新增表文件）

| 日期 | 变更 | 位置 |
|---|---|---|
| 2026-08-21 | 昵称审核：nickname_audit 表 + user 加列（nickname_pending/nickname_status） | `sql/tables/nickname_audit.sql` |
| 2026-08-21 | 平台公告：announcement 表 | `sql/tables/announcement.sql` |
| 历史 | 评论/意向/举报/申诉/日志等表 | 已并入 `sql/schema.sql`（15 张基础表） |
