# 部署与运维手册（草案）

本手册覆盖「一键启动、验证、备份恢复、回滚、常见故障处置」，对应《设计文档》第十、十二章。当前仓库前端工程尚未提交，Nginx 与静态资源部分待 C 交付后再补入。

## 1. 环境要求

| 组件 | 版本 | 说明 |
| --- | --- | --- |
| Docker | 24+ | 含 Docker Compose v2（`docker compose`） |
| 内存 | 4 GB 以上 | MySQL + Redis + 后端三个容器 |
| 端口 | 8080 | 可用 `API_PORT` 覆盖 |

> Docker Hub 在部分网络不可达，仓库统一使用 `public.ecr.aws` 镜像站，与 `docker-compose.dev.yml` 保持一致。

## 2. 一键启动

```bash
cp deploy/.env.example deploy/.env      # 首次启动前执行
docker compose -f deploy/docker-compose.yml up -d --build
docker compose -f deploy/docker-compose.yml ps
```

首次启动时 MySQL 容器会自动执行：

1. `docs/sql/schema.sql`（20 张表）
2. `docs/sql/init_data.sql`（角色、权限点、系统参数、测试账号、基础商品）

数据卷已存在时不会重复执行，需要重建请先 `down -v`。

测试账号：

| 账号 | 手机号 | 口令 |
| --- | --- | --- |
| 管理员 | 13800000001 | 123456 |
| 运营 | 13800000002 | 123456 |
| 会员 | 13800000003 | 123456 |

## 3. 部署后验证

```bash
bash deploy/smoke-test.sh
```

脚本检查三项：`/actuator/health`、`/api/v1/health`、`/api/v1/doc`。全部通过才视为部署成功。

手工核对：

```bash
docker exec linli-mysql mysql -uroot -proot123456 -N \
  -e "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='linli_fresh';"
docker exec linli-redis redis-cli ping
```

## 4. 环境分离

| 配置项 | dev | test / prod |
| --- | --- | --- |
| 启动方式 | `docker-compose.dev.yml`（仅依赖） | `docker-compose.yml`（含 API） |
| Profile | `dev` | `prod`，全部走环境变量 |
| 数据库口令 | 本地明文默认值 | `deploy/.env`，不提交 |
| 日志级别 | DEBUG + SQL 打印 | INFO |

`deploy/.env` 已在 `.gitignore` 中忽略，禁止提交真实口令。

## 5. 备份与恢复

```bash
# 备份（全量）
docker exec linli-mysql mysqldump --single-transaction -uroot -proot123456 \
  linli_fresh > backup_$(date +%Y%m%d_%H%M).sql

# 恢复
docker exec -i linli-mysql mysql -uroot -proot123456 linli_fresh < backup_20261026_0300.sql
```

对齐《设计文档》10.5：RTO ≤ 30 分钟、RPO ≤ 5 分钟。正式环境还需开启 binlog 并每日 03:00 定时备份，保留 7 天。

恢复后必须核对库存恒等式（总库存 = 可用 + 锁定 + 已售），确认无负库存与重复扣减后再恢复对外服务。

## 6. 回滚

```bash
# 回滚到上一个镜像版本
IMAGE_TAG=<上一个提交短哈希> docker compose -f deploy/docker-compose.yml up -d api
bash deploy/smoke-test.sh
```

回滚只重建 `api` 容器，不动数据卷；数据库结构变更保持向后兼容，不做结构回退。

## 7. 常见故障

| 现象 | 排查 | 处理 |
| --- | --- | --- |
| `api` 反复重启 | `docker compose logs -f api` | 多为数据库未就绪或口令不符 |
| 表不存在 | `docker compose ps`、查看 mysql 日志 | 首启脚本失败，`down -v` 后重建 |
| 端口占用 | `netstat -ano \| findstr :8080` | 修改 `.env` 中 `API_PORT` |
| 镜像拉取失败 | 检查网络 | 确认使用 `public.ecr.aws` 镜像 |
| 健康检查失败 | `docker exec linli-api wget -qO- localhost:8080/actuator/health` | 查看应用启动日志中的异常 |

## 8. 本地开发注意（JDK 版本）

项目编译目标为 Java 17，CI 固定使用 JDK 17。若本机安装的是 JDK 23 及以上版本，javac 默认关闭注解处理，Lombok 会失效并报「找不到符号 log / getProductId」。

处理方式（任选其一）：

1. 安装 JDK 17 并将 `JAVA_HOME` 指向它（推荐，与 CI 一致）。
2. 临时用参数运行：

```bash
mvn -B test -Dmaven.compiler.proc=full -Dlombok.version=1.18.42
```

## 9. 待补

- Nginx 反向代理与前后端静态资源（等 C 的前端产物，D11）。
- Prometheus + Grafana 监控编排（P1）。
- 服务器部署 job 与镜像仓库推送（D13）。
