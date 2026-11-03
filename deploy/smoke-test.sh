#!/usr/bin/env bash
# ============================================================
# 邻里鲜生 · 全栈部署冒烟检查（后端 + C 端 + B 端）
# 用法：bash deploy/smoke-test.sh [API_BASE] [MALL_BASE] [ADMIN_BASE]
# 默认：http://localhost:8080 / :8081 / :8082
# ============================================================
set -euo pipefail

API_BASE="${1:-http://localhost:8080}"
MALL_BASE="${2:-http://localhost:8081}"
ADMIN_BASE="${3:-http://localhost:8082}"
RETRIES="${SMOKE_RETRIES:-30}"
INTERVAL="${SMOKE_INTERVAL:-2}"

pass=0
fail=0

# check <名称> <base> <path> <期望片段>
check() {
  local name="$1"
  local base="$2"
  local path="$3"
  local expect="$4"
  local body

  for _ in $(seq 1 "${RETRIES}"); do
    if body="$(curl -fsS --max-time 5 "${base}${path}" 2>/dev/null)"; then
      if [[ -z "${expect}" || "${body}" == *"${expect}"* ]]; then
        echo "[PASS] ${name}  ${base}${path}"
        pass=$((pass + 1))
        return 0
      fi
    fi
    sleep "${INTERVAL}"
  done

  echo "[FAIL] ${name}  ${base}${path}"
  fail=$((fail + 1))
  return 1
}

echo "==> 后端 : ${API_BASE}"
echo "==> C 端 : ${MALL_BASE}"
echo "==> B 端 : ${ADMIN_BASE}"

check "Actuator 健康检查" "${API_BASE}" "/actuator/health" '"status":"UP"'
check "业务健康检查" "${API_BASE}" "/api/v1/health" '"code":0'
check "OpenAPI 文档" "${API_BASE}" "/api/v1/doc" 'openapi'

check "C 端首页" "${MALL_BASE}" "/" '<div id="app">'
check "C 端 API 反代" "${MALL_BASE}" "/api/v1/health" '"code":0'
check "C 端 SPA 回退" "${MALL_BASE}" "/orders" '<div id="app">'

check "B 端首页" "${ADMIN_BASE}" "/" '<div id="app">'
check "B 端 API 反代" "${ADMIN_BASE}" "/api/v1/health" '"code":0'
check "B 端 SPA 回退" "${ADMIN_BASE}" "/orders" '<div id="app">'

echo "---------------------------------------------"
echo "通过 ${pass} 项，失败 ${fail} 项"

if [[ "${fail}" -gt 0 ]]; then
  echo "冒烟检查未通过，请查看："
  echo "  docker compose -f deploy/docker-compose.yml ps"
  echo "  docker compose -f deploy/docker-compose.yml logs -f api web-mall web-admin"
  exit 1
fi

echo "冒烟检查全部通过"