# SQL 目录规范

> 项目数据库脚本统一管理规范。**新增表一律按本规范执行。**

## 目录结构

```
sql/
├── schema.sql              # 基础表（15 张核心表，保持稳定，一般不再修改）
├── demo-data.sql           # 演示数据（用户/分类/商品/订单等）
├── migration-*.sql         # 历史增量迁移（已部署环境升级用，只增不改）
├── tables/                 # ★ 新增表目录：每张新表一个独立文件
│   ├── announcement.sql    #   例：平台公告表
│   └── nickname_audit.sql  #   例：昵称修改审核表
└── c2c.sql                 # 本地数据库导出（含真实数据，gitignore 排除，不上传）
```

## 新增一张表的步骤（规范）

1. **在 `sql/tables/` 新建 `表名.sql`**，包含：
   - 文件头注释：表名 + 用途说明 + 执行方式
   - `CREATE TABLE IF NOT EXISTS ...`（幂等，重复执行安全）
   - 涉及已有表加字段时，用 `ADD COLUMN IF NOT EXISTS`
2. **演示数据**：如需模拟数据，在文件末尾加"演示数据（可选执行）"段，
   用 `INSERT ... SELECT ... WHERE NOT EXISTS(...)` 保证幂等（表有数据就不重复插）
3. **不要改 `schema.sql`**（它是基础表，保持稳定；只有基础表结构变更才动它并同步更新头部注释的表清单）
4. 如需增量升级已部署环境，再复制一份到 `sql/migration-YYYYMMDD-xxx.sql`（可选）

## 约定

- 所有脚本幂等（`IF NOT EXISTS`），可重复执行
- 编码 UTF-8，支持中文注释
- 字段注释必须中文，写清业务含义
- 表名/字段名统一 `snake_case`

## 全新部署：导入顺序（服务器 / 宝塔）

按顺序执行即可（新增表文件含幂等演示数据，重复执行安全）：

```bash
# 方式一：命令行（服务器上，先建库）
mysql -uroot -p -e "CREATE DATABASE IF NOT EXISTS c2c DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
mysql -uroot -p c2c < schema.sql            # ① 基础表 15 张
mysql -uroot -p c2c < tables/announcement.sql     # ② 新增表 + 演示数据
mysql -uroot -p c2c < tables/nickname_audit.sql   # ③ 新增表 + 演示数据
mysql -uroot -p c2c < demo-data.sql         # ④ 基础表演示数据（可选）

# 方式二：宝塔面板
# 数据库 → c2c → 管理 → SQL 标签，依次粘贴上面各文件内容执行
# （文件内容可从仓库直接复制，全部 UTF-8）
```

> 已在运行的环境升级：只执行新增的 `tables/xxx.sql` 即可（幂等），无需动 schema.sql。
