#!/usr/bin/env bash
# ============================================================
# 邻里鲜生 · 初始化数据库
# 前提：docker compose -f deploy/docker-compose.dev.yml up -d 已启动
# 用法：bash deploy/init-db.sh
# ============================================================
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
DB_CONTAINER="linli-mysql"
DB_USER="root"
DB_PASS="root123456"
DB_NAME="linli_fresh"

if ! docker ps --format '{{.Names}}' | grep -q "^${DB_CONTAINER}$"; then
  echo "[ERROR] 未找到容器 ${DB_CONTAINER}，请先执行："
  echo "  docker compose -f deploy/docker-compose.dev.yml up -d"
  exit 1
fi

echo "==> 等待 MySQL 就绪"
for i in $(seq 1 60); do
  if docker exec "${DB_CONTAINER}" mysqladmin ping -u"${DB_USER}" -p"${DB_PASS}" --silent >/dev/null 2>&1; then
    echo "MySQL 已就绪"
    break
  fi
  echo "  等待中... (${i}/60)"
  sleep 2
  if [[ "${i}" -eq 60 ]]; then
    echo "[ERROR] MySQL 启动超时" >&2
    exit 1
  fi
done

echo "==> 导入建表脚本 schema.sql"
docker exec -i "${DB_CONTAINER}" mysql -u"${DB_USER}" -p"${DB_PASS}" "${DB_NAME}" < "${ROOT_DIR}/docs/sql/schema.sql"

echo "==> 导入初始化数据 init_data.sql"
docker exec -i "${DB_CONTAINER}" mysql -u"${DB_USER}" -p"${DB_PASS}" "${DB_NAME}" < "${ROOT_DIR}/docs/sql/init_data.sql"

echo "==> 校验"
docker exec "${DB_CONTAINER}" mysql -u"${DB_USER}" -p"${DB_PASS}" -N -e \
  "SELECT CONCAT('tables=', COUNT(*)) FROM information_schema.tables WHERE table_schema='${DB_NAME}';"
docker exec "${DB_CONTAINER}" mysql -u"${DB_USER}" -p"${DB_PASS}" -N -e \
  "SELECT CONCAT('users=', COUNT(*)) FROM ${DB_NAME}.t_user;"
docker exec "${DB_CONTAINER}" mysql -u"${DB_USER}" -p"${DB_PASS}" -N -e \
  "SELECT CONCAT('products=', COUNT(*)) FROM ${DB_NAME}.t_product;"

echo "==> 完成。测试账号：13800000001 / 13800000002 / 13800000003，口令均为 123456"
