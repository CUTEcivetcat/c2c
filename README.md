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

## 📜 许可证

本项目仅供学习交流使用，请勿用于商业用途。


## 📊 数据库表结构

共 18 张表，210 个字段。

### `admin_log`

|字段|类型|注释|可空|默认|
|---|---|---|---|---|
|`id`|bigint(20)|ID|N||
|`operator_id`|bigint(20)|操作人ID|Y||
|`operator_role`|tinyint(4)|操作人角色|Y|0|
|`action`|varchar(50)|操作类型|N||
|`target_type`|varchar(20)|目标类型|N||
|`target_id`|bigint(20)|目标ID|Y||
|`detail`|varchar(1000)|详情|Y||
|`created_at`|datetime|操作时间|N|CURRENT_TIMESTAMP|
### `announcement`

|字段|类型|注释|可空|默认|
|---|---|---|---|---|
|`id`|bigint(20)|ID|N||
|`title`|varchar(200)|标题|N||
|`content`|text|内容|N||
|`type`|tinyint(4)|类型 1公告2公约3通知|N|1|
|`status`|tinyint(4)|状态 1已发布0已下架|N|1|
|`pinned`|tinyint(4)|是否置顶|N|0|
|`is_force`|tinyint(4)|是否强制弹窗|N|0|
|`min_seconds`|int(11)|最低停留秒|N|0|
|`scroll`|tinyint(4)|是否滚动展示|N|1|
|`show_on_publish`|tinyint(4)|发布页展示|N|0|
|`created_by`|bigint(20)|发布人ID|Y||
|`created_at`|datetime|发布时间|N|CURRENT_TIMESTAMP|
|`updated_at`|datetime|更新时间|N|CURRENT_TIMESTAMP|
### `banner`

|字段|类型|注释|可空|默认|
|---|---|---|---|---|
|`id`|bigint(20)|ID|N||
|`title`|varchar(100)|标题|Y||
|`image_url`|varchar(500)|图片地址|N||
|`link_url`|varchar(500)|跳转链接|Y||
|`sort_order`|int(11)|排序|Y|0|
|`status`|tinyint(4)|状态 1启用0停用|Y|1|
|`created_at`|datetime|创建时间|Y|CURRENT_TIMESTAMP|
|`updated_at`|datetime|更新时间|Y|CURRENT_TIMESTAMP|
### `category`

|字段|类型|注释|可空|默认|
|---|---|---|---|---|
|`id`|bigint(20)|ID|N||
|`name`|varchar(50)|分类名|N||
|`parent_id`|bigint(20)|父分类ID|Y|0|
|`level`|tinyint(4)|层级 1一级2二级|Y|1|
|`sort_order`|int(11)|排序|Y|0|
|`icon_url`|varchar(500)|图标|Y||
|`created_at`|datetime|创建时间|Y|CURRENT_TIMESTAMP|
### `conversation`

|字段|类型|注释|可空|默认|
|---|---|---|---|---|
|`id`|bigint(20)|ID|N||
|`user1_id`|bigint(20)|用户1ID|N||
|`user2_id`|bigint(20)|用户2ID|N||
|`product_id`|bigint(20)|关联商品ID|Y||
|`last_message`|varchar(500)|最后一条消息|Y||
|`last_message_time`|datetime|最后消息时间|Y||
|`user1_unread`|int(11)|用户1未读数|Y|0|
|`user2_unread`|int(11)|用户2未读数|Y|0|
|`created_at`|datetime|创建时间|Y|CURRENT_TIMESTAMP|
|`updated_at`|datetime|更新时间|Y|CURRENT_TIMESTAMP|
### `favorite`

|字段|类型|注释|可空|默认|
|---|---|---|---|---|
|`id`|bigint(20)|ID|N||
|`user_id`|bigint(20)|用户ID|N||
|`product_id`|bigint(20)|商品ID|N||
|`created_at`|datetime|收藏时间|Y|CURRENT_TIMESTAMP|
### `message`

|字段|类型|注释|可空|默认|
|---|---|---|---|---|
|`id`|bigint(20)|ID|N||
|`conversation_id`|bigint(20)|会话ID|N||
|`sender_id`|bigint(20)|发送者|N||
|`receiver_id`|bigint(20)|接收者|N||
|`content`|text|消息内容|N||
|`message_type`|tinyint(4)|类型 1文字2图片3系统|Y|1|
|`extra`|json|额外数据JSON|Y||
|`is_read`|tinyint(4)|是否已读|Y|0|
|`created_at`|datetime(3)|发送时间|Y|CURRENT_TIMESTAMP(3)|
### `nickname_audit`

|字段|类型|注释|可空|默认|
|---|---|---|---|---|
|`id`|bigint(20)|ID|N||
|`user_id`|bigint(20)|用户ID|N||
|`old_nickname`|varchar(50)|原昵称|Y||
|`new_nickname`|varchar(50)|新昵称|N||
|`status`|tinyint(4)|状态 0待审核1通过2拒绝|N|0|
|`reason`|varchar(500)|处理说明|Y||
|`handled_by`|bigint(20)|处理人ID|Y||
|`created_at`|datetime|申请时间|N|CURRENT_TIMESTAMP|
|`handled_at`|datetime|处理时间|Y||
### `order`

|字段|类型|注释|可空|默认|
|---|---|---|---|---|
|`id`|bigint(20)|ID|N||
|`order_no`|varchar(32)|订单号|N||
|`buyer_id`|bigint(20)|买家ID|N||
|`seller_id`|bigint(20)|卖家ID|N||
|`product_id`|bigint(20)|商品ID|N||
|`product_title`|varchar(200)|商品标题(快照)|N||
|`product_image`|varchar(500)|商品封面(快照)|Y||
|`price`|decimal(10,2)|商品价格|N||
|`freight_amount`|decimal(10,2)|运费|Y|0.00|
|`total_amount`|decimal(10,2)|订单总额|N||
|`address_id`|bigint(20)|地址ID|N||
|`address_snapshot`|json|地址快照JSON|Y||
|`status`|tinyint(4)|状态 0待支付1已支付2已发货3已收货4已完成5已取消|N|0|
|`payment_method`|varchar(20)|支付方式|Y||
|`escrow`|decimal(10,2)|平台托管金|Y||
|`payment_time`|datetime|支付时间|Y||
|`ship_company`|varchar(50)|快递公司|Y||
|`ship_no`|varchar(50)|快递单号|Y||
|`ship_time`|datetime|发货时间|Y||
|`receive_time`|datetime|收货时间|Y||
|`complete_time`|datetime|完成时间|Y||
|`cancel_time`|datetime|取消时间|Y||
|`cancel_reason`|varchar(500)|取消原因|Y||
|`buyer_rated`|tinyint(4)|买家已评价|Y|0|
|`seller_rated`|tinyint(4)|卖家已评价|Y|0|
|`created_at`|datetime|创建时间|Y|CURRENT_TIMESTAMP|
|`updated_at`|datetime|更新时间|Y|CURRENT_TIMESTAMP|
### `product`

|字段|类型|注释|可空|默认|
|---|---|---|---|---|
|`id`|bigint(20)|ID|N||
|`seller_id`|bigint(20)|卖家ID|N||
|`category_id`|bigint(20)|分类ID|N||
|`title`|varchar(200)|标题|N||
|`description`|text|描述|Y||
|`price`|decimal(10,2)|售价|N||
|`original_price`|decimal(10,2)|原价|Y||
|`condition`|tinyint(4)|成色 1全新2几乎全新3轻微使用4明显使用|N||
|`status`|tinyint(4)|状态 1在售2下架3违规4已售|Y|1|
|`review_reason`|varchar(500)|违规原因|Y||
|`freight_type`|tinyint(4)|运费类型 1包邮2买家承担|Y|1|
|`freight_amount`|decimal(10,2)|运费|Y|0.00|
|`view_count`|int(11)|浏览量|Y|0|
|`favorite_count`|int(11)|收藏数|Y|0|
|`location`|varchar(100)|所在地|Y||
|`created_at`|datetime|创建时间|Y|CURRENT_TIMESTAMP|
|`updated_at`|datetime|更新时间|Y|CURRENT_TIMESTAMP|
### `product_appeal`

|字段|类型|注释|可空|默认|
|---|---|---|---|---|
|`id`|bigint(20)|ID|N||
|`product_id`|bigint(20)|商品ID|N||
|`seller_id`|bigint(20)|卖家ID|N||
|`appeal_reason`|varchar(1000)|申诉理由|Y||
|`images`|varchar(2000)|图片|Y||
|`status`|tinyint(4)|状态 1待审核2通过3驳回|N|1|
|`appeal_count`|tinyint(4)|申诉次数|N|1|
|`handled_by`|bigint(20)|处理人|Y||
|`reply`|varchar(500)|处理回复|Y||
|`handled_at`|datetime|处理时间|Y||
|`created_at`|datetime|创建时间|N|CURRENT_TIMESTAMP|
|`updated_at`|datetime|更新时间|N|CURRENT_TIMESTAMP|
### `product_comment`

|字段|类型|注释|可空|默认|
|---|---|---|---|---|
|`id`|bigint(20) unsigned|ID|N||
|`product_id`|bigint(20)|商品ID|N||
|`user_id`|bigint(20)|用户ID|N||
|`parent_id`|bigint(20)|父评论ID|N|0|
|`content`|varchar(1000)|内容|N||
|`status`|tinyint(4)|状态 1正常0删除|N|1|
|`created_at`|datetime|创建时间|N|CURRENT_TIMESTAMP|
### `product_image`

|字段|类型|注释|可空|默认|
|---|---|---|---|---|
|`id`|bigint(20)|ID|N||
|`product_id`|bigint(20)|商品ID|N||
|`url`|varchar(500)|图片地址|N||
|`sort_order`|int(11)|排序|Y|0|
|`is_cover`|tinyint(4)|是否封面 1是0否|Y|0|
|`created_at`|datetime|创建时间|Y|CURRENT_TIMESTAMP|
### `product_intent`

|字段|类型|注释|可空|默认|
|---|---|---|---|---|
|`id`|bigint(20) unsigned|ID|N||
|`product_id`|bigint(20)|商品ID|N||
|`seller_id`|bigint(20)|卖家ID|N||
|`buyer_id`|bigint(20)|买家ID|N||
|`message`|varchar(500)|留言|Y||
|`expected_price`|decimal(10,2)|期望价|Y||
|`status`|tinyint(4)|状态 1待回复2已回复3已成交4已关闭|N|1|
|`seller_reply`|varchar(500)|卖家回复|Y||
|`created_at`|datetime|创建时间|N|CURRENT_TIMESTAMP|
|`updated_at`|datetime|更新时间|N|CURRENT_TIMESTAMP|
### `rating`

|字段|类型|注释|可空|默认|
|---|---|---|---|---|
|`id`|bigint(20)|ID|N||
|`order_id`|bigint(20)|订单ID|N||
|`rater_id`|bigint(20)|评价人|N||
|`rated_user_id`|bigint(20)|被评价人|N||
|`role`|tinyint(4)|角色 1买家2卖家|N||
|`score`|tinyint(4)|评分 1~5|N||
|`comment`|varchar(500)|评价内容|Y||
|`tags`|json|标签JSON|Y||
|`created_at`|datetime|评价时间|Y|CURRENT_TIMESTAMP|
### `report`

|字段|类型|注释|可空|默认|
|---|---|---|---|---|
|`id`|bigint(20)|ID|N||
|`reporter_id`|bigint(20)|举报人|N||
|`product_id`|bigint(20)|商品ID|N||
|`report_type`|tinyint(4)|类型 1违规2虚假3其他|N|1|
|`reason`|varchar(500)|举报原因|Y||
|`images`|varchar(2000)|图片|Y||
|`status`|tinyint(4)|状态 1待处理2已处理3驳回|N|1|
|`handled_by`|bigint(20)|处理人|Y||
|`handle_remark`|varchar(500)|处理备注|Y||
|`handled_at`|datetime|处理时间|Y||
|`created_at`|datetime|创建时间|N|CURRENT_TIMESTAMP|
|`updated_at`|datetime|更新时间|N|CURRENT_TIMESTAMP|
### `user`

|字段|类型|注释|可空|默认|
|---|---|---|---|---|
|`id`|bigint(20)|用户ID|N||
|`username`|varchar(100)|用户名(登录账号,唯一)|N||
|`password`|varchar(255)|密码(加密存储)|N||
|`phone`|varchar(20)|手机号|Y||
|`email`|varchar(100)|邮箱|Y||
|`openid`|varchar(64)|微信openid|Y||
|`email_verified`|tinyint(4)|邮箱是否验证|Y|0|
|`avatar_url`|varchar(500)|头像地址|Y||
|`nickname`|varchar(50)|昵称|Y||
|`nickname_pending`|varchar(50)|待审核昵称|Y||
|`nickname_status`|tinyint(4)|昵称状态 0正常1审核中|N|0|
|`bio`|text|个人简介|Y||
|`gender`|tinyint(4)|性别 0保密1男2女|Y|0|
|`status`|tinyint(4)|状态 1正常0封禁|Y|1|
|`role`|tinyint(4)|角色 0普通1管理员2审核|Y|0|
|`login_source`|varchar(20)|注册来源 浏览email/wechat/phone|Y|email|
|`reputation_score`|decimal(3,1)|信誉分 0~5|Y|5.0|
|`balance`|decimal(10,2)|账户余额|N|0.00|
|`last_login_at`|datetime|最近登录|Y||
|`created_at`|datetime|注册时间|Y|CURRENT_TIMESTAMP|
|`updated_at`|datetime|更新时间|Y|CURRENT_TIMESTAMP|
### `user_address`

|字段|类型|注释|可空|默认|
|---|---|---|---|---|
|`id`|bigint(20)|ID|N||
|`user_id`|bigint(20)|所属用户|N||
|`receiver_name`|varchar(50)|收货人|N||
|`phone`|varchar(20)|收货电话|N||
|`province`|varchar(50)|省|Y||
|`city`|varchar(50)|市|Y||
|`district`|varchar(50)|区县|Y||
|`detail`|varchar(255)|详细地址|N||
|`postal_code`|varchar(10)|邮编|Y||
|`is_default`|tinyint(4)|是否默认地址 1是0否|Y|0|
|`created_at`|datetime|创建时间|Y|CURRENT_TIMESTAMP|
|`updated_at`|datetime|更新时间|Y|CURRENT_TIMESTAMP|
### `wallet_log`

|字段|类型|注释|可空|默认|
|---|---|---|---|---|
|`id`|bigint(20)|ID|N||
|`user_id`|bigint(20)|用户ID|N||
|`type`|varchar(20)|类型 recharge/pay/refund/receive|N||
|`amount`|decimal(10,2)|金额(正收入负支出)|N||
|`balance_before`|decimal(10,2)|变动前余额|N||
|`balance_after`|decimal(10,2)|变动后余额|N||
|`order_id`|bigint(20)|关联订单ID|Y||
|`remark`|varchar(200)|备注|Y||
|`created_at`|datetime|创建时间|N|CURRENT_TIMESTAMP|
