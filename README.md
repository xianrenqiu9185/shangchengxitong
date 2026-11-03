# 邻里鲜生社区超市商城系统

面向社区「最后一公里」的生鲜日用即时零售商城，支持**到家配送**与**到店自提**。项目完整覆盖需求分析、架构设计、编码实现、测试与部署运维。

## 技术栈

| 层 | 技术 |
| --- | --- |
| 后端 | Java 17 + Spring Boot 3 + MyBatis-Plus |
| 前端 | Vue 3 + Vite + Pinia；C 端 Vant 4、B 端 Element Plus |
| 数据库 | MySQL 8.0（InnoDB，utf8mb4） |
| 缓存 | Redis 7（会话、权限、商品缓存） |
| 部署 | Docker + Docker Compose + Nginx |
| 监控 | Spring Boot Actuator（Prometheus / Grafana 可选） |

## 快速开始

```bash
cp deploy/.env.example deploy/.env
docker compose -f deploy/docker-compose.yml up -d --build
bash deploy/smoke-test.sh
```

| 入口 | 地址 |
| --- | --- |
| C 端商城 | http://localhost:8081 |
| B 端后台 | http://localhost:8082 |
| 后端 API | http://localhost:8080 |
| OpenAPI | http://localhost:8080/api/v1/doc/ui.html |

测试账号：`13800000001`（管理员）、`13800000002`（运营）、`13800000003`（会员），口令均为 `123456`。

详细步骤见 [部署与运维手册](deploy/README.md)。

## 文档索引

| 文档 | 说明 |
| --- | --- |
| [需求规格说明书](docs/需求规格说明书.md) | 业务范围、角色权限、核心闭环、接口与验收指标 |
| [设计文档](docs/设计文档.md) | 架构、ADR、核心交易、部署测试与可观测性 |
| [数据库设计文档](docs/数据库设计文档.md) | 20 张表的字段、索引与初始化数据 |
| [项目分工与进度计划](docs/项目分工与进度计划.md) | 角色 RACI、两周排期与验收清单 |
| [测试报告](docs/测试报告.md) | 测试范围、用例结论、压测与部署验证 |
| [演示视频脚本](docs/演示视频脚本.md) | 15 分钟录屏分镜与讲解要点 |
| [部署与运维手册](deploy/README.md) | 一键启动、备份恢复、回滚、故障处置 |
| [B 第一周实现说明](docs/B-第一周实现说明.md) | 用户、商品、购物车的实现记录 |
| [B 第二周实现说明](docs/B-第二周实现说明.md) | 售后、评价、后台与安全用例 |

## 仓库结构

```text
shangchengxitong/
├── backend/                    # Spring Boot 后端
├── frontend/
│   ├── linli-mall-web/         # C 端商城（Nginx 8081）
│   └── linli-admin-web/        # B 端后台（Nginx 8082）
├── deploy/                     # Compose、Nginx、冒烟、切换、运维手册
├── test/k6/                    # 性能与一致性测试脚本
├── docs/                       # 需求 / 设计 / 数据库 / 计划 / 报告
│   └── sql/                    # 建表、初始化数据、增量迁移
└── .github/workflows/ci.yml    # 后端 + 前端矩阵 + 镜像构建
```

## 常用命令

```bash
# 查看当前使用哪个数据库（本机 MySQL 与容器互斥）
.\deploy\switch-db.ps1

# 切到项目容器数据库
.\deploy\switch-db.ps1 project

# 切回本机 MySQL
.\deploy\switch-db.ps1 local

# 冒烟检查
bash deploy/smoke-test.sh

# k6 冒烟
k6 run test/k6/smoke.js
```

## 分支规范

| 分支 | 用途 | 说明 |
| --- | --- | --- |
| `main` | 可发布版本 | 受保护，需合并请求 |
| `develop` | 集成分支 | 功能合并到此 |
| `feature/*` | 功能开发 | 如 `feature/deploy-final` |
| `fix/*` | 缺陷修复 | 如 `fix/stock-release` |

提交信息采用 `type(scope): subject`，如 `feat(order): lock stock on submit order`。

## 核心约定

- 鉴权为方案 B：不透明随机 Token 存 Redis，支持即时注销与禁用。
- 库存为三态模型（可用 / 锁定 / 已售），原子条件更新，禁止超卖。
- 关键写接口携带 `Idempotency-Key`，配合幂等表与业务唯一键。
- 金额以「分」为单位存 `BIGINT`。
- 接口统一 `Result`、分页、错误码与 traceId。

## 安全约定

- 禁止提交密码、Token、数据库口令、私钥。
- 本地配置使用 `.env`（已被忽略）。
- 核心交易代码必须经 A 评审后合并。
