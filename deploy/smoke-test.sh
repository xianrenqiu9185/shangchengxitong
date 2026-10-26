#!/usr/bin/env bash
# ============================================================
# 邻里鲜生 · 部署后冒烟检查
# 用法：bash deploy/smoke-test.sh [BASE_URL]
# 默认：http://localhost:8080
# ============================================================
set -euo pipefail

BASE_URL="${1:-http://localhost:8080}"
RETRIES="${SMOKE_RETRIES:-30}"
INTERVAL="${SMOKE_INTERVAL:-2}"

pass=0
fail=0

check() {
  local name="$1"
  local path="$2"
  local expect="$3"
  local body

  for _ in $(seq 1 "${RETRIES}"); do
    if body="$(curl -fsS --max-time 5 "${BASE_URL}${path}" 2>/dev/null)"; then
      if [[ -z "${expect}" || "${body}" == *"${expect}"* ]]; then
        echo "[PASS] ${name}  ${path}"
        pass=$((pass + 1))
        return 0
      fi
    fi
    sleep "${INTERVAL}"
  done

  echo "[FAIL] ${name}  ${path}"
  fail=$((fail + 1))
  return 1
}

echo "==> 冒烟检查目标：${BASE_URL}"
check "Actuator 健康检查" "/actuator/health" '"status":"UP"'
check "业务健康检查" "/api/v1/health" '"code":0'
check "OpenAPI 文档" "/api/v1/doc" 'openapi'

echo "---------------------------------------------"
echo "通过 ${pass} 项，失败 ${fail} 项"

if [[ "${fail}" -gt 0 ]]; then
  echo "冒烟检查未通过，请查看："
  echo "  docker compose -f deploy/docker-compose.yml logs -f api"
  exit 1
fi

echo "冒烟检查全部通过"
