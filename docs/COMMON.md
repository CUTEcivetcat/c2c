# 闲小鱼 · 公共能力与可复用组件详解

> 本文档详细介绍项目中**可复用到其他项目**的公共模块：登录体系、公共基础设施、前端公共组件、公共工具等。

---

## 一、登录与认证体系

### 1.1 JWT 令牌（JwtUtils）

**文件**：`src/main/java/com/c2c/common/utils/JwtUtils.java`

- **算法**：HS256（HMAC-SHA256 对称签名）
- **载荷**：`sub`（用户ID）、`role`（角色声明）、`iat`（签发时间）、`exp`（过期时间）
- **密钥**：外部配置 `jwt.secret` 注入，开发/生产分离

```java
JwtUtils.createToken(userId, Map.of("role", role), secret, expiration)  // 签发(带角色)
JwtUtils.createToken(userId, secret, expiration)                        // 签发(简单)
JwtUtils.parseToken(token, secret)      // 解析 → Claims
JwtUtils.getUserId(token, secret)       // 取用户ID
JwtUtils.isExpired(token, secret)       // 是否过期
JwtUtils.validate(token, secret)        // 是否有效
```

### 1.2 认证过滤器（AuthTokenFilter）

**文件**：`src/main/java/com/c2c/common/filter/AuthTokenFilter.java`

1. 请求进来 → 判断路径是否在白名单（无需登录）
2. 取 `Authorization: Bearer <token>`
3. `JwtUtils.validate` 验签 + 查黑名单（Redis `logout:token:`）
4. 通过 → 写入请求头 `X-User-Id` → 放行
5. 失败 → 返回 401

**白名单示例**：`/user/login`、`/user/register`、`/product/list`、`/banner/list`、`/v3/api-docs`...

**Controller 取值**：`@RequestHeader("X-User-Id") Long userId`

### 1.3 登录方式

| 方式 | 接口 | 说明 |
|---|---|---|
| 密码登录 | `POST /user/login`（loginType=1） | 智能识别手机号/邮箱 |
| 验证码登录 | `POST /user/login`（loginType=2） | 不存在自动注册 |
| 微信登录 | `POST /user/wechat-login` | code2session → openid 自动注册 |### 1.4 验证码机制

**文件**：`UserServiceImpl.sendVerificationCodeAndReturn`

- Redis key：`sms:code:{account}`，5 分钟有效
- 邮箱 → 异步 SMTP 发送（HTML 模板）
- 手机 → 开发模式在响应中回显 code（便于本地联调）

### 1.5 微信登录（小程序）

**文件**：`WechatService.java`

- code2session 换 openid：`https://api.weixin.qq.com/sns/jscode2session`
- **mock 模式**（`wechat.mock=true`）：根据 code 派生固定 openid，本地无 AppSecret 也能联调
- 生产：配置真实 `appid/secret`，关闭 mock

### 1.6 绑定与来源

| 能力 | 说明 |
|---|---|
| 绑定邮箱 | `POST /user/bind-email`，验证码校验 + 防占用 |
| 绑定手机 | `POST /user/bind-phone` |
| 登录来源 | `user.login_source`（email/wechat/phone），管理端可查 |

### 1.7 登出与刷新

- **登出**：token 加入 Redis 黑名单 `logout:token:{token}`，直到过期
- **刷新**：`refreshToken`（30天）存 Redis `refresh:token:{userId}`

---

## 二、公共基础设施

### 2.1 统一响应 R

```json
{ "code": 200, "message": "success", "data": {...}, "timestamp": 1787431525169 }
```

| 方法 | 用途 |
|---|---|
| `R.ok(data)` | 成功 |
| `R.badRequest(msg)` | 400 参数错误 |
| `R.fail(msg)` | 500 失败 |

### 2.2 全局异常处理

- `BusinessException(code, msg)`：业务异常，前端直接展示 msg
- 参数校验（@Valid）异常：统一转成可读提示
- 兜底异常：记录日志，返回"服务器繁忙"（不泄露堆栈）

### 2.3 接口常量 ApiPath

所有 URL 常量集中管理，Swagger 自动生成中文文档。新增接口流程：
1. ApiPath 加常量 → 2. Controller 用常量 → 3.（公开接口）白名单加路径

### 2.4 配置体系（安全）

```
src/main/resources/application*.yml   → 脱敏占位符（进仓库）
config/application-pro.yml            → 真实值（gitignore 排除）
```

- `${VAR:默认值}` 支持环境变量覆盖
- 数据库密码 / JWT 密钥 / 邮箱授权码 / 服务器 IP 绝不入仓
- 日志 `./logs/app.log` 相对路径，环境变量可覆盖

### 2.5 日志

- `logging.file.name: ${APP_LOG_FILE:./logs/app.log}`
- 生产：宝塔设 `APP_LOG_FILE` 环境变量指向固定目录---

## 三、前端公共组件

### 3.1 axios 请求封装（request.js）

```js
request.get(url, params)
request.post(url, data)
request.put(url, data)
request.delete(url)
```
自动带 token、401 跳登录、错误统一 toast。

### 3.2 通用 UI 组件

| 组件 | 文件 | 用途 |
|---|---|---|
| ErrorPage | `components/common/ErrorPage.vue` | 错误/空状态页（error/warn/empty 三色） |
| NotFoundView | `views/NotFoundView.vue` | 404 页（动画数字 + 返回） |
| SkeletonCard | `components/common/SkeletonCard.vue` | 加载骨架屏（shimmer） |
| LoadingSpinner | `components/common/LoadingSpinner.vue` | 三环加载动画 |
| ProductCover | `components/common/ProductCover.vue` | 图片加载失败兜底占位 |
| PageBackBar | `components/layout/PageBackBar.vue` | 页面返回栏 |

### 3.3 全局样式覆盖（uiverse 风格）

**文件**：`App.vue` 的 `<style>`（全局）

统一风格：卡片圆角 16px + 悬停阴影、按钮渐变 + 上浮、输入框聚焦光晕、弹窗圆角、分页圆角。

### 3.4 常用技巧

| 技巧 | 说明 |
|---|---|
| `keep-alive + onActivated` | 页面返回自动刷新（首页购买后回来自动更新） |
| `ResizeObserver` | 侧边栏折叠时 ECharts 自动 resize |
| `URL.createObjectURL` | 图片上传前本地预览 |

---

## 四、公共工具类

| 类 | 用途 |
|---|---|
| `JwtUtils` | JWT 签发/解析/校验 |
| `SensitiveWordUtils` | 敏感词过滤（昵称/标题） |
| `MapUtils.of(...)` | 便捷构建 Map（最多 5 对） |
| `R` | 统一响应 |
| `ApiPath` | 接口常量 |

---

## 五、复用清单（直接搬走）

| 复用项 | 目标项目 | 改动量 |
|---|---|---|
| JWT + AuthTokenFilter | 任何 Spring Boot 后端 | 改密钥即可 |
| R + 全局异常 | 任何 Spring Boot 后端 | 直接复制 |
| 验证码（Redis+邮箱） | 需要登录注册的后端 | 改模板 |
| axios 封装 | 任何 Vue 前端 | 改 baseURL |
| ErrorPage/404/骨架屏 | 任何 Vue 前端 | 直接复制 |
| uiverse 全局样式 | 任何 Element Plus 前端 | 直接复制 |
| 微信登录 mock 模式 | 小程序项目 | 改配置 |
| escrow 担保交易 | C2C/担保平台 | 适配业务 |
| wallet_log 流水表 | 资金/积分体系 | 直接建表 |

---

## 六、目录速查

```
src/main/java/com/c2c/common/
├── constant/ApiPath.java       # 接口常量
├── exception/                  # 业务异常 + 全局处理
├── filter/AuthTokenFilter.java # JWT 认证过滤器
├── result/R.java               # 统一响应
├── utils/                      # JwtUtils / MapUtils / SensitiveWordUtils
├── config/                     # 全局配置
└── enums/                      # 业务枚举

frontend/src/
├── api/request.js              # axios 封装
├── components/common/          # 通用组件
├── components/layout/          # 布局组件
├── stores/                     # Pinia 状态
└── utils/                      # 业务工具
```