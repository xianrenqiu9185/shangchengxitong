# 后端工程（linli-mall）

Java 17 + Spring Boot 3 + MyBatis-Plus + MySQL 8.0 + Redis 7。

## 启动依赖

```bash
# 在仓库根目录
docker compose -f deploy/docker-compose.dev.yml up -d
bash deploy/init-db.sh
```

## 启动应用

```bash
cd backend
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

或打包后运行：

```bash
mvn -DskipTests package
java -jar target/linli-mall-0.0.1-SNAPSHOT.jar --spring.profiles.active=dev
```

默认端口 `8080`。

## 验证

```bash
# 健康检查
curl http://localhost:8080/api/v1/health

# Actuator
curl http://localhost:8080/actuator/health

# OpenAPI JSON / Swagger UI
open http://localhost:8080/api/v1/doc
open http://localhost:8080/api/v1/doc/ui.html
```

健康检查返回示例：

```json
{
  "code": 0,
  "message": "OK",
  "data": { "status": "UP", "app": "linli-mall" },
  "traceId": "…",
  "timestamp": 1730000000000
}
```

## 目录结构

```text
src/main/java/com/linli/mall/
├── LinliMallApplication.java
├── common/
│   ├── api/          Result、PageResult、ErrorCode
│   ├── exception/    BizException、GlobalExceptionHandler
│   ├── trace/        TraceIdFilter（X-Trace-Id → MDC）
│   ├── auth/         Token 鉴权拦截器与权限点契约
│   ├── config/       WebConfig、MyBatis-Plus 分页
│   └── web/          HealthController
├── user/             用户与鉴权域
├── product/          商品与分类域
├── cart/             购物车域
├── inventory/        库存域
├── order/            订单域
├── payment/          支付与退款域
├── aftersale/        售后域
├── marketing/        营销（优惠券）域
├── review/           评价域
├── system/           系统与权限域
└── statistics/       统计看板域
```

每个业务域内部按 `controller → service → mapper → entity/dto` 组织。

## 统一约定

- **返回体**：`Result<T>{code,message,data,traceId,timestamp}`，`code=0` 成功。
- **分页**：入参 `pageNum` / `pageSize`，出参 `PageResult<T>{list,total,pageNum,pageSize}`。
- **异常**：业务异常抛 `BizException(ErrorCode)`，由 `GlobalExceptionHandler` 统一转换。
- **traceId**：请求头 `X-Trace-Id` 透传，缺失则生成；日志与返回体均携带。
- **金额**：接口与数据库统一用「分」的整数（`long` / `BIGINT`）。
- **时间**：`Asia/Shanghai`，JSON 格式 `yyyy-MM-dd HH:mm:ss`。
- **逻辑删除**：MyBatis-Plus 全局字段 `deleted`（0 正常，1 删除）；无该字段的表不受影响。
- **Mapper**：接口需标注 `@Mapper`，放在 `com.linli.mall.<domain>.mapper`。

## 鉴权契约（方案 B）

- 登录成功后写入 Redis：`auth:session:{token} → userId:userType:roleId`（TTL 2 小时，滑动续期）。
- 请求头：`Authorization: Bearer <token>`。
- `AuthInterceptor` 负责：取 Token → 查 Redis → 写入 `UserContext` → 校验 `@RequirePermission`。
- 权限点校验由 `PermissionChecker` 提供实现（用户/RBAC 模块负责）；未提供实现时仅做登录校验并打印警告。
- 公开路径已在 `WebConfig` 排除：`/api/v1/health`、`/api/v1/auth/**`、`/api/v1/products/**`、`/api/v1/categories/**`、`/api/v1/doc/**`。

## 开发配置

`application-dev.yml` 默认连接：

- MySQL：`localhost:3306/linli_fresh`，`root / root123456`
- Redis：`localhost:6379`

生产配置 `application-prod.yml` 使用环境变量 `DB_URL / DB_USERNAME / DB_PASSWORD / REDIS_HOST / REDIS_PORT`，禁止把生产口令写入代码。

## 下一步（B 负责）

1. 实现注册/登录/注销与 Token 会话写入。
2. 实现 `PermissionChecker`（基于 `t_role_permission` + 缓存）。
3. 商品、购物车、售后、评价模块。
