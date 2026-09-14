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

# ---- 日志目录：确保存在并固定到项目根 logs/（Spring Boot 不会自动创建父目录，
#      且相对路径 ./logs 依赖启动 cwd，systemd/宝塔 下 cwd 不同会导致日志丢失）----
# 默认 /softservice/c2c/logs（项目根目录），与部署文档一致；可用 APP_LOG_DIR 覆盖
LOG_DIR="${APP_LOG_DIR:-$(dirname "${APP_DIR}")/logs}"
mkdir -p "${LOG_DIR}"
export APP_LOG_FILE="${APP_LOG_FILE:-${LOG_DIR}/app.log}"

exec "${JAVA_BIN}" "${JAVA_OPTS[@]}" -jar "${JAR_FILE}"
