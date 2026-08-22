# 闲小鱼 · C2C 二手交易平台

> 🔄 让闲置流转起来 —— 一个前后端分离的 C2C 二手交易系统（Spring Boot 单体 + Vue3 双前端）。

---

## ✨ 项目简介

**闲小鱼** 是一个校园/社区场景的二手交易平台，支持商品发布与浏览、买卖双方站内私信、下单交易与评价、违规举报与审核申诉、管理员后台等完整闭环。代码结构清晰、注释齐全（中文），适合作为 **Java Web 全栈学习项目** 参考。

## 🚀 功能特性

| 模块 | 功能 |
|---|---|
| 👤 用户 | 邮箱/手机注册登录、验证码登录、找回密码、收货地址、个人主页、信誉评分、**微信小程序登录**、**绑定邮箱** |
| 🛍️ 商品 | 发布/编辑/上下架、两级分类、多图上传、搜索筛选排序、**搜索历史/热门词/联想**、商品评论、购买意向、**猜你喜欢、浏览历史** |
| 💬 即时通讯 | 买卖双方站内私信、会话列表、未读角标、已读回执、微信风格聊天界面 |
| 📦 订单 | 下单/支付/**余额担保交易**/发货/收货/取消 全状态流转、订单快照 |
| ⭐ 评价 | 交易完成后买卖双方互评、**信誉平均分自动计算** |
| 📢 公告 | 强制弹窗/停留秒数/滚动显示/**发布页右侧展示**，管理员全流程管理 |
| 💰 钱包 | **余额充值、平台担保托管（支付/确认收款/退款）、资金流水** |
| ⚑ 审核 | 举报/违规下架/申诉/昵称审核，审核员工作台 |
| 🖥️ 管理后台 | 仪表盘（ECharts、**7日/30日/自定义趋势**、**待处理卡片**）、用户/商品/订单/审核/公告/**轮播图**管理、**登录来源**、**侧边栏折叠**、**uiverse 风格** |
| 📄 接口文档 | Swagger UI 在线文档（中文注释），接口 URL 常量集中管理 |

## 🛠️ 技术栈

| 层 | 技术 |
|---|---|
| 后端 | Java 8 · Spring Boot 2.7 · MyBatis-Plus 3.5 · MySQL 8 · Redis · JWT · Hutool · Lombok · SpringDoc(Swagger) |
| 用户端前端 | Vue 3 · Vite 5 · Vue Router · Pinia · Element Plus · ESLint |
| 管理端前端 | Vue 3 · Element Plus · ECharts · ESLint |
| 微信小程序 | 原生微信小程序（完整闭环：登录/发布/订单/钱包） |
| 部署 | Maven 打包 jar · Nginx 静态托管 · 宝塔面板 |

## 📁 项目结构

```
c2c/
├── src/main/java/com/c2c/        # 后端源码
│   ├── common/                   # 通用：统一响应R/异常/JWT过滤器/常量/配置
│   ├── user/                     # 用户模块：登录注册/地址/邮箱/微信登录
│   ├── product/                  # 商品模块：商品/分类/评论/意向/上传
│   ├── order/                    # 订单模块：下单/状态流转/MQ事件
│   ├── im/                       # 即时通讯：会话/消息/未读
│   ├── favorite/ rating/         # 收藏 / 评价
│   ├── review/                   # 举报/申诉/审核工作台
│   ├── wallet/                   # 余额钱包：充值/支付/退款/流水
│   ├── banner/                   # 首页轮播图管理
│   ├── announcement/             # 平台公告管理
│   └── admin/                    # 管理端接口：认证/统计/角色
├── src/main/resources/           # 配置（脱敏占位符版，真实值走外部 config/）
├── frontend/                     # 用户端前端（Vite 5173）
├── admin-frontend/               # 管理端前端（Vite 5174，base /admin/）
├── blog/                         # 博客前端（Vite 5175）
├── weapp/                        # 微信小程序（完整功能：登录/发布/订单/钱包等）
├── sql/                          # init.sql 全量建表(18张) / data.sql 全量演示数据
├── config/                       # 外部真实配置（含敏感值，已被 .gitignore 排除）
├── start/                        # 前端一键启动脚本（bat/sh）
├── deploy/                       # 部署脚本与示例配置（脱敏）
├── BUGS.md                       # 已知问题记录
```

## 🏃 快速开始（本地开发）

### 环境要求
- JDK 8+、Maven 3.6+
- MySQL 8、Redis（本地或远程均可）
- Node.js 16+、npm

### 1. 数据库
```sql
-- 建库 + 建表 + 演示数据（任选其一）
source sql/init.sql;      -- 全量建表（18 张表 + 字段注释，会清空库）
source sql/data.sql;      -- 全量演示数据（用户/分类/商品/订单/公告/钱包等）
```

### 2. 后端
```bash
# 复制配置模板并填入你的真实值（数据库/Redis/邮箱授权码/JWT密钥）
cp config/application.example.yml config/application-pro.yml

mvn clean package -DskipTests
java -jar target/c2c-monolith.jar
# 启动后访问 http://localhost:8080/api/v1/swagger-ui/index.html 查看接口文档
```

> 配置说明：`src/main/resources/application*.yml` 为脱敏占位符版（可公开），
> 真实敏感值放在 `config/` 外部目录（已被 .gitignore 排除，不进仓库）。
> Spring Boot 启动时自动加载 jar 同目录 `config/` 下的配置并逐 key 覆盖。

### 3. 前端（任选一种）
```bash
# 方式一：一键脚本（Windows 双击 start-frontend.bat 或 Git Bash 运行）
bash start/start-frontend.sh frontend    # 用户端  http://localhost:5173
bash start/start-frontend.sh admin       # 管理端  http://localhost:5174
bash start/start-frontend.sh all         # 全部

# 方式二：手动
cd frontend && npm install && npm run dev
```

### 4. 代码检查（可选）
```bash
cd frontend && npm run lint   # ESLint 检查 Vue 项目
```

### 5. 微信小程序（可选）
```bash
# 微信开发者工具 → 导入项目 → 选择 weapp/ 目录
# 修改 weapp/app.js 中的 serverUrl 为你的后端地址
# 需填入 AppID（或使用测试号）；勾选"不校验合法域名"
# 功能：微信登录、首页/分类/搜索、发布、详情、订单、钱包、收藏、绑定邮箱
```

## 📄 API 文档

部署后访问（接口前缀 `/api/v1`）：
- Swagger UI：`http://<host>:8080/api/v1/swagger-ui/index.html`
- 原始 JSON：`http://<host>:8080/api/v1/v3/api-docs`

> 除标注「公开」的接口外，均需请求头 `Authorization: Bearer <token>`（登录接口返回）。

## ☁️ 部署（生产）

1. `mvn clean package -DskipTests` 打包后端；`npm run build` 构建用户端前端；`cd admin-frontend && npm run build` 构建管理端
2. 后端 jar 与 `config/application-prod.yml` 放到服务器，`SPRING_PROFILES_ACTIVE=prod` 启动（参考 `deploy/start-backend.sh`）
3. 前端 `frontend/dist/` 和 `admin-frontend/dist/` 分别交由 Nginx 托管（参考 `deploy/` 下示例配置）

## 🔒 安全说明

- 真实密码/密钥/IP 一律放在本地 `config/`（gitignore 排除），仓库内仅保留占位符模板
- 同步/上传仓库前请自查：`config/`、`*.sql` 导出、部署文档等含真实信息的文件不得入仓

## 🤝 可提供部署帮助
![img.png](img.png)


## 📜 许可证

本项目仅供学习交流使用，请勿用于商业用途。
