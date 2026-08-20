# C2C 启动脚本

本目录存放项目**本地启动脚本**，方便一键运行前端开发服务器。

## 前端启动（start-frontend）

启动 Vite 开发服务器，支持热更新。首次运行会自动 `npm install`。

**Windows（Git Bash）**：
```bash
bash start/start-frontend.sh            # 用户端
bash start/start-frontend.sh admin      # 管理端
bash start/start-frontend.sh blog       # 博客
bash start/start-frontend.sh all        # 全部
```

**Windows（双击 .bat）**：
- 双击 `start-frontend.bat` → 用户端
- 或命令行：`start-frontend.bat admin` / `blog` / `all`

**Linux / macOS**：同上 `.sh` 方式。

## 三个前端的默认端口

| 项目 | 目录 | 端口 |
|---|---|---|
| 用户端 | `frontend/` | http://localhost:5173 |
| 管理端 | `admin-frontend/` | http://localhost:5174 |
| 博客 | `blog/` | http://localhost:5175 |

> 端口见各项目 `vite.config.js`（默认 5173，多项目启动时 Vite 自动 +1 递增）。

## 前端打包（build-frontend）

打包产物输出到各项目 `dist/` 目录，可配合 `deploy/upload-frontend.sh` 上传服务器。

**Windows（双击 .bat）**：
- 双击 `build-frontend.bat` → 打包用户端
- 或命令行：`build-frontend.bat admin` / `blog` / `all`

**Linux / macOS / Git Bash**：
```bash
bash start/build-frontend.sh            # 用户端
bash start/build-frontend.sh admin      # 管理端
bash start/build-frontend.sh all        # 全部
```

首次运行自动 `npm install`，之后直接 `npm run build`。

## 后端启动

后端是 Spring Boot，先打包再运行（详见 `deploy/docs/` 或根目录 README）：

```bash
mvn clean package -DskipTests
java -jar target/c2c-monolith.jar
```

后端默认端口 `8080`，接口前缀 `/api/v1`。前端 `vite.config.js` 已配置代理，开发时前端请求 `/api` 自动转发到后端。

## 配置文件说明

敏感配置（数据库密码、邮箱授权码、JWT 密钥等）全部放在 **`config/` 目录** 的外部配置文件中，由 Spring Boot 启动时自动加载，**不打包进 jar**、**不会上传到仓库**（已被 `.gitignore` 排除）。

详见 `config/application.example.yml` 中的使用说明。
