# 闲小鱼 微信小程序

C2C 二手交易 · 微信小程序端（微信登录、绑定邮箱、商品浏览、余额下单）

## 功能
- 微信登录：wx.login 拿 code -> 后端 /user/wechat-login 自动登录/注册
- 绑定邮箱：登录后通过邮箱验证码绑定（可用于找回密码/密码登录）
- 商品浏览：首页列表、商品详情、余额下单
- 我的：个人信息、登录来源展示、退出

## 本地运行

### 后端
java -jar target/c2c-monolith.jar
需配置 wechat.mock=true（本机默认），mock 模式派生 openid，免真实 AppSecret。

### 数据库
- user 表需含 openid、login_source 两列，变更见 db-update.sql（开发库已应用，生产前需执行）

### 微信开发者工具
1. 导入项目，选择本 weapp/ 目录
2. 填 AppID（测试号即可）
3. 勾选「不校验合法域名」
4. 编译运行

推荐 serverUrl：http://127.0.0.1:8080/api/v1（可改 app.js）