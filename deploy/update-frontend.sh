#!/usr/bin/env bash
# =====================================================
# C2C 前端【一键打包 + 上传】
# 在本地电脑（Git Bash）运行：
#   bash deploy/update-frontend.sh
# 相当于：build-frontend.sh + upload-frontend.sh --clean
# =====================================================
set -Eeuo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"

echo -e "\033[1;32m[步骤 1/2] 本地打包\033[0m"
bash "$SCRIPT_DIR/build-frontend.sh"

echo -e "\033[1;32m[步骤 2/2] 上传服务器（先清空旧文件）\033[0m"
bash "$SCRIPT_DIR/upload-frontend.sh" --clean

echo -e "\033[1;32m全部完成，去浏览器验证吧。\033[0m"
