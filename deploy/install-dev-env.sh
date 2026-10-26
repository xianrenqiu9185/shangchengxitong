#!/usr/bin/env bash
# ============================================================
# 邻里鲜生 · 本地开发环境安装脚本（Ubuntu 24.04+ / 26.04）
# 安装：OpenJDK 17、Maven、Docker Engine、Docker Compose v2
# 用法：sudo bash deploy/install-dev-env.sh
# ============================================================
set -euo pipefail

if [[ "${EUID}" -ne 0 ]]; then
  echo "[ERROR] 请使用 sudo 运行：sudo bash deploy/install-dev-env.sh" >&2
  exit 1
fi

if ! command -v apt-get >/dev/null 2>&1; then
  echo "[ERROR] 当前系统不是 Debian/Ubuntu，请手动安装。" >&2
  exit 1
fi

echo "==> 1/5 更新 apt 索引"
apt-get update

echo "==> 2/5 安装 OpenJDK 17、Maven、Docker、Compose v2"
DEBIAN_FRONTEND=noninteractive apt-get install -y \
  openjdk-17-jdk \
  maven \
  docker.io \
  docker-compose-v2

echo "==> 3/5 启用并启动 Docker"
systemctl enable --now docker

echo "==> 4/5 将当前用户加入 docker 组"
TARGET_USER="${SUDO_USER:-}"
if [[ -n "${TARGET_USER}" && "${TARGET_USER}" != "root" ]]; then
  usermod -aG docker "${TARGET_USER}"
  echo "已将 ${TARGET_USER} 加入 docker 组"
else
  echo "[WARN] 未检测到 SUDO_USER，跳过用户组配置"
fi

echo "==> 5/5 版本检查"
java -version || true
mvn -v | head -3 || true
docker -v || true
docker compose version || true

cat <<'EOF'

============================================================
安装完成。请执行以下步骤：

1) 让 docker 组生效（二选一）：
   - 重新登录终端
   - 或执行：newgrp docker

2) 启动 MySQL 8.0 + Redis 7：
   cd /home/user8800/shangchengxitong
   docker compose -f deploy/docker-compose.dev.yml up -d

3) 初始化数据库：
   bash deploy/init-db.sh

4) 查看状态：
   docker compose -f deploy/docker-compose.dev.yml ps
   docker exec -it linli-mysql mysql -uroot -proot123456 -e "SELECT VERSION();"
   docker exec -it linli-redis redis-cli INFO server | grep redis_version
============================================================
EOF
