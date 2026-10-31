# CI 流水线说明

> 目标：每次 push 到 main 自动验证「后端可编译 + 单测通过 + 双前端可构建」，把低级错误挡在合并前。

## 当前状态

| 项 | 命令 | 说明 |
|---|---|---|
| 后端 | `mvn -B clean package -Dtest='*,!MySqlConnectionTest,!RedisConnectionTest' -DfailIfNoTests=false` | JDK **17**（Lombok 1.18.30 不支持 JDK 21+）；排除依赖内网数据库/Redis 的连接状态测试 |
| 用户端前端 | `npm ci && npm run lint && npm run build` | 目录 `frontend/` |
| 管理端前端 | `npm ci && npm run build` | 目录 `admin-frontend/`（未配置 lint 脚本） |

## 方式一：GitHub Actions（仓库同步到 GitHub 时自动生效）

直接使用仓库根目录 `.github/workflows/ci.yml`，无需额外配置。

## 方式二：Gitee Go（当前远程是 Gitee，推荐）

Gitee Go 在网页端配置（流水线配置暂不完全是"仓库内文件驱动"）：

1. Gitee 仓库页 → **DevOps** → **Gitee Go** → 新建流水线；
2. 选择 **Maven 构建** 模板，触发方式选「推送触发」（main 分支）；
3. 构建环境：**Maven 3.9 + JDK 17**（关键！默认 JDK 8 可以编译，但保持 17 与本地一致）；
4. Maven 命令替换为上面表格中的后端命令；
5. 再加一个「Shell」阶段执行前端两条命令（或装 Node 20 后分两步执行）；
6. 保存并「立即验证」——故意改坏一个测试，确认流水线会变红（有效性验证）。

## 验收标准

- [ ] push 一次提交，流水线全绿；
- [ ] 故意破坏一个单测再 push，流水线变红；
- [ ] 故意破坏前端代码再 push，前端阶段变红。
