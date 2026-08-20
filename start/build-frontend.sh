#!/usr/bin/env bash
# =============================================================
# C2C 前端打包脚本（Git Bash / Linux / macOS）
#
# 用法：
#   bash start/build-frontend.sh            # 打包用户端 frontend
#   bash start/build-frontend.sh admin      # 打包管理端 admin-frontend
#   bash start/build-frontend.sh blog       # 打包博客 blog
#   bash start/build-frontend.sh all        # 打包全部
#
# 产物输出到各项目 dist/ 目录，可配合 deploy/upload-frontend.sh 上传。
# 首次运行会自动执行 npm install，之后直接 npm run build。
# =============================================================
set -Eeuo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
MODE="${1:-frontend}"

build_one() {
  local name="$1" dir="$2"
  echo ""
  echo "=============================================="
  echo " 打包 ${name}  (${dir}/)"
  echo "=============================================="
  cd "$ROOT/$dir" || { echo "[错误] 目录不存在: $ROOT/$dir"; exit 1; }
  # 没有 node_modules 时先安装依赖
  if [[ ! -d node_modules ]]; then
    echo "首次打包，正在安装依赖（npm install）..."
    npm install
  fi
  npm run build
  echo ""
  echo "✅ ${name} 打包成功：${ROOT}/${dir}/dist/"
}

case "$MODE" in
  frontend)  build_one "用户端" "frontend" ;;
  admin)     build_one "管理端" "admin-frontend" ;;
  blog)      build_one "博客" "blog" ;;
  all)
    build_one "用户端" "frontend"
    build_one "管理端" "admin-frontend"
    build_one "博客" "blog"
    ;;
  *)
    echo "用法: bash start/build-frontend.sh [frontend|admin|blog|all]"
    exit 1
    ;;
esac
