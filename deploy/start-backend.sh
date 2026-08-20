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

cd "${APP_DIR}"
exec "${JAVA_BIN}" "${JAVA_OPTS[@]}" -jar "${JAR_FILE}"
