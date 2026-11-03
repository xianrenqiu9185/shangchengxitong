# 部署与运维手册

邻里鲜生全栈部署说明，对应《设计文档》第十、十二章。当前版本覆盖 MySQL、Redis、后端 API 与两个前端（C 端商城、B 端后台）。

## 1. 服务与端口

| 服务 | 容器 | 端口 | 对外访问 |
| --- | --- | --- | --- |
| MySQL | linli-mysql | 内部 3306 | 不对外 |
| Redis | linli-redis | 内部 6379 | 不对外 |
| 后端 API | linli-api | 8080 | http://localhost:8080 |
| C 端商城 | linli-web-mall | 8081 → 容器 80 | http://localhost:8081 |
| B 端后台 | linli-web-admin | 8082 → 容器 80 | http://localhost:8082 |

两个前端各自独立构建、独立 Nginx，不做 `base` 路径改造：

- 前端请求统一走相对路径 `/api/v1`；
- 每个 Nginx 把 `/api/` 反代到 `api:8080`；
- 路由回退 `try_files $uri $uri/ /index.html`，刷新子路由不 404。

## 2. 环境要求

| 组件 | 版本 |
| --- | --- |
| Docker Engine | 24+ |
| Docker Compose | v2（`docker compose`） |
| 内存 | 6 GB 以上（五个容器 + 两套前端构建） |
| 端口 | 8080 / 8081 / 8082 可用 |

> Docker Hub 在部分网络不可达，Compose 默认使用 `public.ecr.aws` 镜像站。CI 环境则通过 build-arg 切换为 Docker Hub，避免 ECR 匿名流量配额。

## 3. 一键启动

```bash
cp deploy/.env.example deploy/.env
docker compose -f deploy/docker-compose.yml up -d --build
docker compose -f deploy/docker-compose.yml ps
```

首次启动时 MySQL 自动执行：

1. `docs/sql/schema.sql`（20 张表）
2. `docs/sql/init_data.sql`（角色、权限点、系统参数、测试账号、基础商品）

数据卷已存在时不会重复执行，需要重建时先 `down -v`。

测试账号：

| 账号 | 手机号 | 口令 |
| --- | --- | --- |
| 管理员 | 13800000001 | 123456 |
| 运营 | 13800000002 | 123456 |
| 会员 | 13800000003 | 123456 |

## 4. 部署后验证

```bash
bash deploy/smoke-test.sh
```

检查 9 项：

- 后端：`/actuator/health`、`/api/v1/health`、`/api/v1/doc`
- C 端：首页、`/api/v1/health` 反代、`/orders` SPA 回退
- B 端：首页、`/api/v1/health` 反代、`/orders` SPA 回退

全部 PASS 才算部署成功。失败时：

```bash
docker compose -f deploy/docker-compose.yml ps
docker compose -f deploy/docker-compose.yml logs -f api web-mall web-admin
```

## 5. 数据库切换（本机 MySQL 与容器互斥）

本机若已安装 MySQL 并占用 3306，需要用脚本在两者之间切换：

```powershell
.\deploy\switch-db.ps1            # 查看当前使用哪个库
.\deploy\switch-db.ps1 project    # 停本机 MySQL80，启动项目容器
.\deploy\switch-db.ps1 local      # 停项目容器，恢复本机 MySQL80
```

切换需要管理员权限，脚本会自动请求提权。项目数据存放在 Docker 卷中，切回本机数据库不会丢失。

## 6. 环境分离

| 配置项 | dev | prod / test |
| --- | --- | --- |
| 启动方式 | `docker-compose.dev.yml`（仅依赖） | `docker-compose.yml`（全栈） |
| Profile | `dev` | `prod`，全部走环境变量 |
| 数据库口令 | 本地默认值 | `deploy/.env`，不入库 |
| 日志级别 | DEBUG + SQL 打印 | INFO |
| 前端模式 | `npm run dev` | 构建产物 + Nginx |

`deploy/.env` 已在 `.gitignore` 中忽略。

## 7. 备份与恢复

### 7.1 全量备份

```bash
docker exec linli-mysql mysqldump --single-transaction -uroot -proot123456 \
  linli_fresh > backup_$(date +%Y%m%d_%H%M).sql
```

建议每日 03:00 执行，保留 7 天；正式环境同时开启 binlog 以支撑 RPO ≤ 5 分钟。

### 7.2 恢复

```bash
docker exec -i linli-mysql mysql -uroot -proot123456 linli_fresh < backup_YYYYMMDD_HHMM.sql
```

### 7.3 恢复后核对

```sql
SELECT product_id, total, locked, sold, (total - locked - sold) AS available
FROM t_inventory
WHERE total < 0 OR locked < 0 OR sold < 0 OR (total - locked - sold) < 0;
```

返回空集才表示库存恒等式成立。核对通过后再恢复对外服务。

## 8. 回滚

```bash
# 切回上一版本镜像（提交短哈希）
IMAGE_TAG=<上一版本> docker compose -f deploy/docker-compose.yml up -d
bash deploy/smoke-test.sh
```

回滚只重建应用容器，不动数据卷；数据库结构变更保持向后兼容，不做结构回退。

## 9. 常见故障

| 现象 | 排查 | 处理 |
| --- | --- | --- |
| api 反复重启 | `docker compose logs -f api` | 多为数据库未就绪或口令不符 |
| 前端 502 | 检查 api 是否 healthy | 等待健康检查通过或重启 api |
| 前端空白页 | 查看浏览器 Network | 确认 `/assets` 返回 200、`/api` 反代正常 |
| 刷新子路由 404 | 检查 nginx.conf 的 try_files | 重新构建前端镜像 |
| 端口冲突 | `netstat -ano \| findstr :8080` | 修改 `.env` 中对应端口 |
| 镜像拉取失败 | 检查网络 | 使用 ECR 镜像或覆盖 `NODE_IMAGE/NGINX_IMAGE` |
| MySQL 连不上 | `test/k6/check-stock.sql` 复核 | 确认本机 MySQL80 已停止、容器 3306 正常 |

## 10. 可观测性

课程环境启用 Spring Boot Actuator：

| 端点 | 说明 |
| --- | --- |
| `/actuator/health` | 存活与依赖状态 |
| `/actuator/metrics` | JVM、HTTP、连接池等指标 |
| `/actuator/info` | 应用信息 |

链路标识：入口生成或透传 `X-Trace-Id`，写入 MDC，并回填响应体 `Result.traceId`。

日志为单行结构化输出，禁止记录密码、Token、密钥与完整手机号。Prometheus / Grafana 为可选增强，本期不强制。

## 11. CI/CD

`.github/workflows/ci.yml` 包含三个 job：

| Job | 内容 |
| --- | --- |
| backend | JDK 17 + MySQL/Redis 服务容器 + `mvn verify` + 归档报告与 jar |
| frontend | 两个前端矩阵，Node 20 + `npm ci` + `npm run build` |
| docker | 构建后端镜像（不推送） |

镜像构建通过 build-arg 使用 Docker Hub 基础镜像，避免 ECR 匿名流量配额。

后续接入服务器部署时，追加 deploy job：登录镜像仓库 → 推送镜像 → ssh 执行 compose → 冒烟 → 失败回滚。

## 12. 本地开发注意

项目编译目标为 Java 17，CI 固定 JDK 17。若本机为 JDK 23+，javac 默认关闭注解处理会导致 Lombok 失效。

处理方式：安装 JDK 17 并设置 `JAVA_HOME`（推荐），或临时使用：

```bash
mvn -B test -Dmaven.compiler.proc=full -Dlombok.version=1.18.42
```
