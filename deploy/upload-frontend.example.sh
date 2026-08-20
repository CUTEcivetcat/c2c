#!/usr/bin/env bash
# =====================================================
# C2C 前端【上传】脚本
# 在本地电脑（Git Bash）运行，把三个 dist 上传到服务器：
#   bash deploy/upload-frontend.sh              # 直接上传（覆盖同名文件）
#   bash deploy/upload-frontend.sh --clean      # 先清空服务器旧文件再上传
# 会提示输入服务器密码（root@YOUR_SERVER_IP:YOUR_SSH_PORT）
# =====================================================
set -Eeuo pipefail

SERVER="root@YOUR_SERVER_IP"
SSH_PORT="YOUR_SSH_PORT"
BASE="/softservice/c2c"
PROJECT_DIR="$(cd "$(dirname "$0")/.." && pwd)"

CLEAN=false
[[ "${1:-}" == "--clean" ]] && CLEAN=true

log() { echo -e "\033[1;32m[上传]\033[0m $*"; }
die() { echo -e "\033[1;31m[错误]\033[0m $*" >&2; exit 1; }

if $CLEAN; then
  log "清空服务器旧文件..."
  ssh -p "$SSH_PORT" "$SERVER" "rm -rf $BASE/frontend/* $BASE/admin-frontend/* $BASE/blog/*" || die "清空失败"
fi

log "上传用户端 -> $BASE/frontend"
scp -P "$SSH_PORT" -r "$PROJECT_DIR/frontend/dist/." "$SERVER:$BASE/frontend/" || die "用户端上传失败"

log "上传管理端 -> $BASE/admin-frontend"
scp -P "$SSH_PORT" -r "$PROJECT_DIR/admin-frontend/dist/." "$SERVER:$BASE/admin-frontend/" || die "管理端上传失败"

log "上传博客 -> $BASE/blog"
scp -P "$SSH_PORT" -r "$PROJECT_DIR/blog/dist/." "$SERVER:$BASE/blog/" || die "博客上传失败"

log "============ 上传完成 ============"
log "用户端: http://YOUR_SERVER_IP/"
log "管理端: http://YOUR_SERVER_IP/admin/"
log "博客:   http://YOUR_SERVER_IP/blog/"
log "静态文件已生效，无需重启服务"
