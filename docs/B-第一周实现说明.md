# 负责人 B 第一周实现说明（Day 2–7）

> 范围：用户与 RBAC、商品与分类、购物车、售后骨架、安全加固。
> 基线：`origin/develop`（`3b8c000`）。
> 结论：B 的第一周 P0 后端目标已完成；评价（P1）与 A 的订单/退款适配器不在本周范围。

---

## 一、完成情况

| 计划日 | 交付内容 | 状态 |
| --- | --- | --- |
| Day 2 | 用户/登录骨架、公共错误码、Token 会话契约 | 已完成 |
| Day 3 | 注册/登录/注销、Redis 会话、RBAC 权限点校验与缓存 | 已完成 |
| Day 4 | 分类/商品公开查询、后台 CRUD、购物车增删改查 | 已完成 |
| Day 5 | 购物车合并/勾选/清空、售后申请与查询骨架 | 已完成 |
| Day 6 | 售后审核、幂等、退款编排、A 模块 Port 契约 | 已完成（待 A 实现 Port） |
| Day 7 | 单元测试、接口与越权自查、交付文档 | 已完成 |

## 二、关键实现

### 2.1 用户与会话

- 注册使用 BCrypt 保存口令，手机号唯一；注册成功直接签发会话。
- 登录连续失败 5 次锁定 15 分钟，成功后清除失败计数。
- 会话采用方案 B：随机 32 字节 Token，Redis 不透明会话，禁止 JWT。
- 会话键 `auth:session:{token}`，值为 `userId:userType:roleId`，TTL 2 小时并在访问时滑动续期。
- 用户会话集合 `auth:user:{userId}:sessions` 支持单端注销与按用户批量踢下线。
- 账号禁用时立即清除该用户全部会话，后续请求返回未登录/账号禁用。
- 个人资料查询/修改；地址增删改查，默认地址在同一用户内唯一。

### 2.2 RBAC

- `@RequirePermission("权限点")` 由 `AuthInterceptor` 统一校验，未登录返回 `10002`，越权返回 `10003`。
- 权限点来源 `t_role_permission → t_permission`，按角色缓存 `auth:perm:{roleId}`，TTL 30 分钟。
- `RolePermissionService.evict(roleId)` 供角色权限变更后主动失效。
- 会员只能访问本人购物车、地址、售后；所有本人数据查询均带 `user_id` 条件。

### 2.3 商品与分类

- 公开接口：启用分类列表、上架商品分页与详情、按分类/关键词筛选。
- 后台接口：分类与商品 CRUD、商品上下架、逻辑删除。
- 分类重名返回 `20006`；分类下存在商品时禁止删除并返回 `20005`。
- 分页参数归一化，`pageSize` 上限 100，关键词最长 32 字符。
- 分类列表缓存 `cache:categories`（1 小时），商品详情缓存 `cache:product:{id}`（10 分钟），写操作主动失效。

### 2.4 购物车

- 依赖唯一键 `(user_id, product_id)` 合并加购，单商品数量封顶 99。
- 单用户最多 100 条购物车项；数量、勾选、删除、清空均校验归属。
- 返回 `{items, count, selectedCount, total}`，商品价格与库存实时读取，不信任前端金额。
- 下架/删除商品仍保留在购物车中并标记不可用，结算总额只累计已勾选且可用的商品。
- 提供 `removeChecked(userId)` 供 A 的下单模块在创建订单后清理勾选项。

### 2.5 售后与幂等

- 状态机：`PENDING(0) → APPROVED(1) → REFUNDING(3) → COMPLETED(4)`，或 `PENDING → REJECTED(2)`。
- 申请售后校验订单归属、订单状态、是否已有处理中售后单。
- 审核使用条件更新，重复审核返回 `40002`，避免并发重复通过。
- 申请与审核均要求 `Idempotency-Key`，首次结果写入 `t_idempotent`，同键成功请求复用结果。
- 退款成功完成售后并回写订单；退款明确失败或结果未知转入 `REFUNDING`，保留重试入口。
- 驳回售后时回滚订单原状态。

## 三、接口清单

统一前缀 `/api/v1`，除标注“公开”外均需 `Authorization: Bearer <token>`。

### 3.1 用户与地址

| 方法 | 路径 | 权限 | 说明 |
| --- | --- | --- | --- |
| POST | `/auth/register` | 公开 | 注册并返回 Token/用户/权限 |
| POST | `/auth/login` | 公开 | 登录并返回 Token/用户/权限 |
| POST | `/auth/logout` | 登录 | 注销当前 Token |
| GET | `/users/me` | 登录 | 当前用户资料与权限 |
| PUT | `/users/me` | `user:profile:update` | 修改昵称 |
| GET | `/users/me/addresses` | `address:info:manage` | 地址列表 |
| POST | `/users/me/addresses` | `address:info:manage` | 新增地址 |
| PUT | `/users/me/addresses/{id}` | `address:info:manage` | 修改地址 |
| DELETE | `/users/me/addresses/{id}` | `address:info:manage` | 删除地址 |

### 3.2 商品与分类

| 方法 | 路径 | 权限 | 说明 |
| --- | --- | --- | --- |
| GET | `/categories` | 公开 | 启用分类 |
| GET | `/products` | 公开 | 上架商品分页（`categoryId/keyword/pageNum/pageSize`） |
| GET | `/products/{id}` | 公开 | 商品详情 |
| GET | `/admin/categories` | `product:category:manage` | 全部分类 |
| POST | `/admin/categories` | `product:category:manage` | 新增分类 |
| PUT | `/admin/categories/{id}` | `product:category:manage` | 修改分类 |
| DELETE | `/admin/categories/{id}` | `product:category:manage` | 删除分类 |
| GET | `/admin/products` | `product:goods:manage` | 商品分页（含下架） |
| POST | `/admin/products` | `product:goods:manage` | 新增商品 |
| PUT | `/admin/products/{id}` | `product:goods:manage` | 修改商品 |
| PUT | `/admin/products/{id}/status` | `product:goods:manage` | 上下架 |
| DELETE | `/admin/products/{id}` | `product:goods:manage` | 删除商品 |

### 3.3 购物车

| 方法 | 路径 | 权限 | 说明 |
| --- | --- | --- | --- |
| GET | `/cart` | `cart:item:manage` | 购物车汇总 |
| POST | `/cart/items` | `cart:item:manage` | 加购/合并 |
| PUT | `/cart/items/{id}` | `cart:item:manage` | 改数量/勾选 |
| DELETE | `/cart/items/{id}` | `cart:item:manage` | 删除一项 |
| DELETE | `/cart/items` | `cart:item:manage` | 清空 |

### 3.4 售后

| 方法 | 路径 | 权限 | 说明 |
| --- | --- | --- | --- |
| POST | `/after-sales` | `after:sale:apply` | 申请售后，需 `Idempotency-Key` |
| GET | `/after-sales` | `after:sale:apply` | 本人售后分页（`status/pageNum/pageSize`） |
| GET | `/after-sales/{saleNo}` | `after:sale:apply` | 本人售后详情 |
| GET | `/admin/after-sales` | `after:sale:audit` | 后台售后分页 |
| POST | `/admin/after-sales/{saleNo}/audit` | `after:sale:audit` | 审核，需 `Idempotency-Key` |

售后申请体：`{orderNo, type: REFUND|RETURN, reason, description}`。
审核体：`{approved, remark}`。

## 四、给 A 的 Port 契约

订单/退款属于 A，B 不直接读写订单表。A 实现以下接口后由 Spring 自动注入；未实现时：

- 申请售后返回 `40003`「订单模块尚未接入」；
- 审核通过后售后单停在 `REFUNDING`，等待退款实现。

### 4.1 `OrderPort`

| 方法 | 语义 |
| --- | --- |
| `OrderInfo findByOrderNoForUser(String orderNo, Long userId)` | 按订单号查询并校验归属；不存在/无权限返回 `null` |
| `Map<Long, OrderInfo> findByIds(Collection<Long> orderIds)` | 批量查询，供售后列表展示订单号 |
| `void markAfterSale(Long orderId, int expectedStatus)` | 条件流转到“售后中”；当前状态不等于 `expectedStatus` 时不得更新 |
| `void restoreStatus(Long orderId, int targetStatus)` | 驳回售后后恢复申请前状态 |
| `void markRefunded(Long orderId)` | 退款成功后流转“已退款”，必须幂等 |

`OrderInfo` 字段：`orderId, orderNo, userId, status, payAmount`（金额单位：分）。
订单状态常量见 `com.linli.mall.aftersale.integration.OrderStatus`（0 待支付、1 待发货、2 待收货、3 已完成、4 已取消、5 售后中、6 已退款）。

### 4.2 `RefundPort`

`RefundResult refund(RefundRequest request)`，其中请求为 `refundNo, saleNo, orderId, amount`。

- 必须按 `refundNo` 幂等，重复调用不得重复退款。
- 返回 `SUCCESS / PROCESSING / FAILED`；业务失败用 `FAILED`，不要用异常表达。
- 抛异常视为结果未知，售后单转 `REFUNDING`，不误判成功。

## 五、缓存与 Redis 键

| 键 | 用途 | TTL |
| --- | --- | --- |
| `auth:session:{token}` | 登录会话 | 2 小时滑动 |
| `auth:user:{userId}:sessions` | 用户全部 Token 集合 | 2 小时滑动 |
| `auth:perm:{roleId}` | 角色权限点 | 30 分钟 |
| `auth:login:fail:{phone}` | 登录失败计数 | 15 分钟 |
| `cache:categories` | 启用分类列表 | 1 小时 |
| `cache:product:{id}` | 商品详情源数据 | 10 分钟 |

## 六、数据契约说明

本周不修改 A 已冻结的 DDL，以下差异在服务层兼容：

- `t_product` 无副标题/单位/原价/销量列：副标题与单位复用 `spec`，原价为 `null`，销量取库存已售数。
- `t_user_address` 无省市区/标签列：省市区拼接进 `detail` 保存，响应中省市区与标签返回空串。
- `t_after_sale` 无 `description` 列：按 `reason | description` 存储，读取时拆分。

若后续 A 审批新增列，可平滑替换上述兼容逻辑，不影响前端字段。

## 七、验证

新增单元测试 6 类、22 个用例，覆盖登录/注册、地址默认唯一与归属、购物车合并与总额、分类重名与占用、幂等结果复用、售后申请/审核/退款编排。

```bash
cd backend
mvn -Dtest=UserServiceTest,AddressServiceTest,CartServiceTest,CategoryServiceTest,IdempotencyServiceTest,AfterSaleServiceTest test
```

测试结果：`Tests run: 22, Failures: 0, Errors: 0, Skipped: 0`。

全量 `mvn test` 还包含需要 MySQL/Redis 的 `InventoryServiceTest`，应在 Docker Compose 与初始化脚本就绪后执行。

说明：

- 本机未启动 MySQL/Redis、未执行 Docker Compose，因此未运行需要真实中间件的 `InventoryServiceTest` 与全链路集成测试。
- 评价模块按计划为 P1，本周未实现。
- A 的订单/支付模块尚未在 `develop` 落地，Port 适配器随 A 的实现补齐后即可联调。
