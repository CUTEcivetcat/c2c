# Bug 记录

> 记录已知问题，后续统一修复。

## 公告未在规定界面展示

- **发现时间**：2026-08-21
- **描述**：部分设置了 `show_on_publish=1` 或 `scroll=1` 的公告，在对应的发布页右侧/首页横幅位置没有正确展示。
- **影响范围**：公告展示功能（发布页公告面板、首页横幅滚动轮播）
- **状态**：⏳ 待定位修复
- **备注**：代码逻辑无报错，可能是前端渲染条件或接口返回数据过滤问题。后续排查时重点检查 `AnnouncementService.listPublish()` 和 `listLatest()` 的查询条件，以及前端 `ProductPublishView` 和 `HomeView` 的公告加载逻辑。

## 部署后日志不生成到项目 log 目录（根因修复）

- **发现时间**：2026-09-14（用户实测复现：服务器部署后项目 `logs/` 目录下没有日志）
- **根因**：① Spring Boot 文件日志（Logback FileAppender）**不会自动创建父目录**，`logs/` 目录不存在时写文件失败；② 配置为相对路径 `./logs`，systemd/宝塔 启动时工作目录不可控，日志落到别处或丢失。
- **修复**：
  - `deploy/start-backend.sh`：启动前 `mkdir -p "${APP_DIR}/logs"` 并注入 `APP_LOG_FILE="${APP_DIR}/logs/app.log"`（绝对路径，与启动 cwd 无关）
  - `application-prod.yml`：默认改为 `${APP_LOG_FILE:/softservice/c2c/app/logs/app.log}`
  - `application.yml`：补充注释，说明目录需存在或由启动脚本创建
- **状态**：✅ 已修复（2026-09-14）

## 网页端购买后商品留存 / 切换页面延迟刷新（已修复）

- **发现时间**：2026-09-14
- **根因**：Vue Router 组件复用——`/product/1` 切到 `/product/2`、`/order/checkout` 切到 `/order/:id` 复用同一组件实例，`onMounted` 不重新触发，页面仍显示旧数据。
- **修复**：`frontend/src/App.vue` 的 `<router-view>` 加 `:key="route.fullPath"`，路由参数变化时强制重建组件并重新拉取数据。
- **状态**：✅ 已修复（2026-09-14）

## 首页搜索框显示不完全 / 轮播位空白（已修复）

- **发现时间**：2026-09-14
- **描述**：小程序首页搜索胶囊底部被裁剪（`.hero` 的 `overflow:hidden` 切掉悬浮元素）；轮播位无数据时显示大片灰底。
- **修复**：搜索胶囊改为嵌入 hero 内（`bottom:28rpx` + hero padding-bottom）；轮播过滤无图项 + 图片加载失败自动隐藏。
- **状态**：✅ 已修复（2026-09-14）

## 编辑资料无法修改 / 分类页点击无反应 / 部分页面加载失败（已修复）

- **发现时间**：2026-09-14
- **根因**：`mine` 页资料入口 data-url 指向未注册路径；`category.js` 的 select 把事件对象当 index 使用；`user/edit`、`user/home` 两页 `require('../../utils/api.js')` 少一级目录导致整页加载失败。
- **修复**：修正跳转路径与 require 相对路径；select 兼容数字/事件对象两种调用并做防御；补齐 11 个缺失样式类。
- **状态**：✅ 已修复（2026-09-14）
