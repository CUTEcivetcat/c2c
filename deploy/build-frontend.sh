#!/usr/bin/env bash
# =====================================================
# C2C 前端【本地打包】脚本
# 在本地电脑（Git Bash）运行：
#   bash deploy/build-frontend.sh
# 打包用户端 / 管理端 / 博客，产物在各自 dist/ 下
# =====================================================
set -Eeuo pipefail

PROJECT_DIR="$(cd "$(dirname "$0")/.." && pwd)"

log() { echo -e "\033[1;32m[打包]\033[0m $*"; }
die() { echo -e "\033[1;31m[错误]\033[0m $*" >&2; exit 1; }

build_one() {
  local name="$1" dir="$2"
  log "打包 ${name} (${dir}) ..."
  ( cd "$PROJECT_DIR/$dir" || die "目录不存在: $PROJECT_DIR/$dir"
    npm run build ) || die "${name} 打包失败"
}

build_one "用户端"   "frontend"
build_one "管理端"   "admin-frontend"
build_one "博客"     "blog"

log "============ 打包完成 ============"
log "产物目录:"
log "  frontend/dist        (用户端)"
log "  admin-frontend/dist  (管理端)"
log "  blog/dist            (博客)"
