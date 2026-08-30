#!/usr/bin/env bash
set -Eeuo pipefail

APP_DIR="/softservice/c2c/app"
JAR_FILE="${APP_DIR}/app.jar"
JAVA_BIN="${JAVA_BIN:-/usr/bin/java}"

JAVA_OPTS=(
  "-Xms256m"
  "-Xmx768m"
  "-XX:+UseG1GC"
  "-Dfile.encoding=UTF-8"
)

if [[ ! -x "${JAVA_BIN}" ]]; then
  echo "Java executable not found: ${JAVA_BIN}" >&2
  exit 1
fi

if [[ ! -f "${JAR_FILE}" ]]; then
  echo "Backend jar not found: ${JAR_FILE}" >&2
  exit 1
fi

# ---- 安全前置校验：JWT_SECRET 必须存在且 ≥32 字节（与 C2cMonolithApplication 启动校验一致）----
if [[ -z "${JWT_SECRET:-}" ]]; then
  echo "❌ 未配置环境变量 JWT_SECRET，后端将拒绝启动。" >&2
  echo "   请先生成随机密钥并注入，例如：" >&2
  echo "     export JWT_SECRET=\"\$(openssl rand -base64 32)\"" >&2
  echo "   然后重新运行本脚本。" >&2
  exit 1
fi
if [[ ${#JWT_SECRET} -lt 32 ]]; then
  echo "❌ JWT_SECRET 长度 ${#JWT_SECRET} 字节，小于 32 字节（HS256 要求），后端将拒绝启动。" >&2
  exit 1
fi

cd "${APP_DIR}"
exec "${JAVA_BIN}" "${JAVA_OPTS[@]}" -jar "${JAR_FILE}"
