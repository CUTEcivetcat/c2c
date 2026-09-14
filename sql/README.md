# SQL 目录说明

> **新部署**只用 `init.sql` + `data.sql`；**已有库升级**用 `update-*.sql`；已并入全量脚本的历史增量在 `xz/` 归档。

## 一、全量脚本（全新环境 / 重灌）

| 文件 | 用途 | 用法 |
|---|---|---|
| `init.sql` | **全量建表**：18 张表 + 全部字段（含 `secondary_pwd` 二级密码、`admin_level` 管理员等级、`admin_perms` 权限码） | `mysql -uroot -p c2c < init.sql` |
| `data.sql` | **全量演示数据**：8 用户（admin 为总管理员 `admin_level=2`）、4 原始商品 + 26 扩充商品（覆盖教材/英语/笔记本电脑/苹果手机/宿舍用品全部分类）、30 商品图、8 收藏、3 意向、地址/订单/公告/钱包等 | `mysql -uroot -p c2c < data.sql` |

```bash
mysql -uroot -p c2c < init.sql   # ① 建表（会清空重建 c2c 库）
mysql -uroot -p c2c < data.sql   # ② 导入演示数据
```

## 二、增量升级脚本（已有库，按需执行）

| 文件 | 用途 | 适用场景 |
|---|---|---|
| `update-20260822-banner.sql` | 轮播图表 | 2026-08 之前的库 |
| `update-20260914-secondary-pwd.sql` | 加 `secondary_pwd` 二级密码字段 | 2026-09-14 之前的库 |
| `update-20260915-admin-levels.sql` | 加 `admin_level` / `admin_perms` 字段 + 存量管理员等级迁移 | 2026-09-15 之前的库（**服务器更新必执行**） |

> ⚠️ 已通过全量脚本（init.sql/data.sql）建的库**不要再执行** `update-*.sql`，否则 ALTER 重复报错。

## 三、归档 `xz/`（已并入全量脚本，备用）

| 文件 | 内容 | 何时仍会用到 |
|---|---|---|
| `xz/data-extra.sql` | 扩充测试数据（用户 5~8、商品 5~30、图片/收藏/意向），幂等 `INSERT IGNORE` | 已有库缺扩充商品时单独补数据 |
| `xz/repair-admin-levels.sql` | admin 等级修复（纯 UPDATE 幂等） | 已有库漏迁移、admin 无权限管理菜单时应急修复（后端登录自愈已兜底，一般不需要） |

```bash
# 已有库补扩充商品（可选）
mysql -uroot -p c2c < sql/xz/data-extra.sql
# 已有库应急修复 admin 等级（可选）
mysql -uroot -p c2c < sql/xz/repair-admin-levels.sql
```

## 四、更新规范

1. 新增表/字段/数据 → **同步进全量脚本**（`init.sql` 加字段、`data.sql` 加数据），保证全新部署一步到位。
2. 已有库需要平滑升级 → 新建 `sql/update-YYYYMMDD-xxx.sql`（幂等写法），文件头注释写明用途，**保留在主目录**。
3. 某增量文件内容**已完全并入全量脚本**后 → 移入 `sql/xz/` 归档（不删除，备查备用），并在本文件第二节登记。
