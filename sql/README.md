# SQL 目录说明

> 新部署只用 `init.sql` + `data.sql`。

## 文件

| 文件 | 用途 | 用法 |
|---|---|---|
| `init.sql` | **全量建表**（18 张表，含基础表 + 新增表 + 资金流水） | `mysql -uroot -p c2c < init.sql` |
| `data.sql` | **全量演示数据**（用户/分类/商品/订单/公告/钱包等） | `mysql -uroot -p c2c < data.sql` |

## 新部署流程

```bash
mysql -uroot -p c2c < init.sql   # ① 建表（会清空重建 c2c 库）
mysql -uroot -p c2c < data.sql   # ② 导入演示数据
```

## 后续更新规范

新增表或数据变更，**不再合并到 `init.sql` / `data.sql`**，而是放在 `sql/` 下：

1. 新建 `sql/update-YYYYMMDD-xxx.sql`
2. 文件头注释写明用途，幂等可用（`IF NOT EXISTS`、`ADD COLUMN` 等）
3. 执行方式：`mysql -uroot -p c2c < sql/update-xxx.sql`