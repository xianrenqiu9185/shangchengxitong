# 邻里鲜生社区超市商城系统

面向社区「最后一公里」的生鲜日用即时零售商城，支持**到家配送**与**到店自提**。本项目为课程实验项目，完整覆盖需求分析、架构设计、编码实现、测试与部署运维。

## 技术栈

| 层 | 技术 |
| --- | --- |
| 后端 | Java 17 + Spring Boot 3 + MyBatis-Plus |
| 前端 | Vue 3 + Vite + Pinia；C 端 Vant 4、B 端 Element Plus |
| 数据库 | MySQL 8.0（InnoDB，utf8mb4） |
| 缓存 | Redis 7（会话、权限、商品缓存） |
| 部署 | Docker + Docker Compose + Nginx |
| 监控 | Spring Boot Actuator + Micrometer + Prometheus + Grafana |

## 仓库结构

```text
shangchengxitong/
├── backend/                  # 后端工程（待脚手架）
├── frontend/
│   ├── linli-mall-web/       # C 端商城（待脚手架）
│   └── linli-admin-web/      # B 端后台（待脚手架）
├── deploy/                   # Docker Compose、Nginx、CI/CD（D 负责）
├── docs/                     # 需求 / 设计 / 数据库 / 项目计划
│   └── sql/
│       ├── schema.sql        # 建表脚本（20 张表）
│       ├── init_data.sql     # 初始化数据与测试账号
│       └── migrations/       # 后续增量迁移脚本
├── .editorconfig
├── .gitattributes
├── .gitignore
└── README.md
```

## 分支规范

| 分支 | 用途 | 说明 |
| --- | --- | --- |
| `main` | 可发布版本 | 受保护，禁止直接推送，需合并请求 |
| `develop` | 集成分支 | 功能合并到此，触发完整流水线 |
| `feature/*` | 功能开发 | 如 `feature/order-create` |
| `fix/*` | 缺陷修复 | 如 `fix/stock-release` |
| `hotfix/*` | 紧急修复 | 从 `main` 拉出，修复后合回 |

## 提交规范

```text
type(scope): subject
```

常用 type：`feat`、`fix`、`docs`、`refactor`、`test`、`chore`。
示例：`feat(order): lock stock on submit order`。

## 快速开始

### 1. 阅读文档

- [需求规格说明书](docs/需求规格说明书.md)
- [设计文档](docs/设计文档.md)
- [数据库设计文档](docs/数据库设计文档.md)
- [项目分工与进度计划](docs/项目分工与进度计划.md)

### 2. 初始化数据库

```bash
mysql -uroot -p -e "CREATE DATABASE linli_fresh DEFAULT CHARSET utf8mb4 COLLATE utf8mb4_0900_ai_ci;"
mysql -uroot -p linli_fresh < docs/sql/schema.sql
mysql -uroot -p linli_fresh < docs/sql/init_data.sql
```

### 3. 测试账号

| 账号 | 手机号 | 口令 | 角色 |
| --- | --- | --- | --- |
| 管理员 | 13800000001 | 123456 | 平台管理员 |
| 运营 | 13800000002 | 123456 | 商家运营 |
| 会员 | 13800000003 | 123456 | 会员 |

> 仅用于开发/测试环境，生产环境必须替换。

### 4. 启动

前端两个工程已搭建完成，默认使用本地 mock，可独立运行：

```bash
cd frontend/linli-mall-web
npm install
npm run dev
```

访问 `http://localhost:5173`，演示账号 `13800000003 / 123456`。

```bash
cd frontend/linli-admin-web
npm install
npm run dev
```

访问 `http://localhost:5174`。后台演示账号：

| 账号 | 手机号 | 口令 | 角色 |
| --- | --- | --- | --- |
| 管理员 | 13800000001 | 123456 | 平台管理员 |
| 运营 | 13800000002 | 123456 | 商家运营 |

更完整的 mock 说明、接口切换方式和构建命令见 [前端工程说明](frontend/README.md)。后端与 Docker 启动命令在对应工程完成后补充。

## 团队分工

| 代号 | 角色 | 负责范围 |
| --- | --- | --- |
| A | 项目经理 + 后端负责人 + 数据库 | 需求、架构、进度、订单/支付/库存/事务/幂等、营销、数据库 |
| B | 后端开发 + 安全 | 用户、RBAC、Token 会话、商品、购物车、售后、评价 |
| C | 前端负责人 | C 端商城、B 端后台、路由与按钮权限、联调 |
| D | 测试 + 运维 + 文档 | 测试、Docker、CI/CD、监控、系统配置/日志/看板、文档、视频 |

> 详细排期与验收清单见 [项目分工与进度计划](docs/项目分工与进度计划.md)。

## 核心约定

- **鉴权**：方案 B，不透明随机 Token 存 Redis，支持即时注销与禁用。
- **库存**：三态模型（可用 / 锁定 / 已售），原子条件更新，禁止超卖。
- **幂等**：关键写接口携带 `Idempotency-Key`，配合幂等表与业务唯一键。
- **接口**：RESTful `/api/v1`，统一 `Result`、分页、错误码与 traceId。
- **金额**：以「分」为单位存 `BIGINT`，禁止浮点。

## 安全约定

- 禁止提交密码、Token、数据库口令、私钥等敏感信息。
- 本地配置使用 `.env.local`（已被 `.gitignore` 忽略）。
- 提交前运行格式化与静态检查；核心交易代码必须经 A 评审。
