# k6 压测脚本

角色 D 的性能与一致性测试脚本，指标口径对齐《需求文档》7.1.1。

## 1. 环境准备

```powershell
# 切到项目数据库（会临时停掉本机 MySQL80）
.\deploy\switch-db.ps1 project

# 启动后端（另开一个窗口）
cd backend
mvn spring-boot:run
```

确认健康检查可用：

```powershell
curl http://127.0.0.1:8080/api/v1/health
```

## 2. 脚本说明

| 脚本 | 用途 | 当前是否可跑 |
| --- | --- | --- |
| `smoke.js` | 健康检查与 OpenAPI 的并发冒烟 | ✅ 现在可跑 |
| `order-concurrency.js` | 下单链路 200 TPS 压测 + 零超卖判定 | ⏳ 等 A 的 `/orders` 接口 |
| `check-stock.sql` | 压测后库存恒等式核对 | ✅ 现在可跑 |

## 3. 运行

```powershell
# 冒烟（10 VU，30 秒）
k6 run test/k6/smoke.js

# 调整并发
k6 run -e VUS=50 -e DURATION=60s test/k6/smoke.js

# 下单压测（接口就绪后）
k6 run -e PHONE=13800000003 -e PASSWORD=123456 -e TARGET_TPS=200 test/k6/order-concurrency.js
```

## 4. 判定标准

| 场景 | 指标 | 依据 |
| --- | --- | --- |
| 健康检查 | P95 ≤ 300ms，错误率 < 1% | 需求 7.1.1 |
| 提交订单 | P95 ≤ 800ms、P99 ≤ 1.5s、≥ 200 TPS | 需求 7.1.1 |
| 库存一致性 | 总 = 可用 + 锁定 + 已售，无负值 | 设计 ADR-003 |
| 幂等 | 同一 Idempotency-Key 只产生一个订单 | 设计 5.6 |

## 5. 库存核对

压测结束后执行：

```powershell
docker exec -i linli-mysql mysql -uroot -proot123456 linli_fresh < test/k6/check-stock.sql
```

两条查询都返回空集才算通过。若返回行，说明出现了负库存或账实不一致，属于 P0 缺陷。

## 6. 收尾

```powershell
# 停容器、恢复本机 MySQL80
.\deploy\switch-db.ps1 local
```
