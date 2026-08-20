#!/usr/bin/env bash
# =============================================================
# C2C 前端启动脚本（Git Bash / Linux / macOS）
#
# 用法：
#   bash start/start-frontend.sh            # 启动用户端 frontend
#   bash start/start-frontend.sh admin      # 启动管理端 admin-frontend
#   bash start/start-frontend.sh blog       # 启动博客 blog
#   bash start/start-frontend.sh all        # 依次启动全部（需分别 Ctrl+C）
#
# 首次运行会自动执行 npm install，之后直接 npm run dev。
# =============================================================
set -Eeuo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
MODE="${1:-frontend}"

start_one() {
  local name="$1" dir="$2"
  echo ""
  echo "=============================================="
  echo " 启动 ${name}  (${dir}/)"
  echo "=============================================="
  cd "$ROOT/$dir" || { echo "[错误] 目录不存在: $ROOT/$dir"; exit 1; }
  # 没有 node_modules 时先安装依赖
  if [[ ! -d node_modules ]]; then
    echo "首次运行，正在安装依赖（npm install）..."
    npm install
  fi
  npm run dev
}

case "$MODE" in
  frontend)  start_one "用户端"   "frontend" ;;
  admin)     start_one "管理端"   "admin-frontend" ;;
  blog)      start_one "博客"     "blog" ;;
  all)
    start_one "用户端" "frontend"
    start_one "管理端" "admin-frontend"
    start_one "博客"   "blog"
    ;;
  *)
    echo "用法: bash start/start-frontend.sh [frontend|admin|blog|all]"
    exit 1
    ;;
esac
