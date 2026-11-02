# 负责人 B 第二周实现说明（Day 8–13）

> 范围：售后联调、商品评价、评价回复、会员管理、角色权限管理、后台账号管理与安全自查。
> 基线：`origin/develop`（`bfb70e4`）。
> 开发分支：`feature/b-week2`。
> 结论：B 的第二周后端功能已完成，纯单元测试 `46/46` 通过；需要 MySQL、Redis 的全链路集成测试待本机中间件凭据配置正确后执行。

---

## 一、完成情况

| 计划日 | 交付内容 | 状态 |
| --- | --- | --- |
| Day 8 | 售后与 A 的订单/退款 Port 联调；商品评价发表、公开查询 | 已完成 |
| Day 9 | 评价回复与后台查询；会员管理；角色权限管理；后台账号管理 | 已完成 |
| Day 10 | 模块级编译、接口字段与前端契约核对 | 已完成 |
| Day 12 | 越权、禁用即失效、角色边界、管理员保护等安全自查 | 已完成 |
| Day 13 | 交付文档与代码评审材料 | 已完成 |

---

## 二、商品评价

### 2.1 接口

| 方法 | 路径 | 权限 | 说明 |
| --- | --- | --- | --- |
| POST | `/api/v1/reviews` | `review:goods:create` | 会员发表评价 |
| GET | `/api/v1/products/{productId}/reviews` | 公开 | 商品评价分页 |
| GET | `/api/v1/admin/reviews` | `review:goods:reply` | 后台评价分页 |
| POST | `/api/v1/admin/reviews/{id}/reply` | `review:goods:reply` | 运营回复评价 |

### 2.2 发表评价

请求体同时兼容设计文档字段和 C 端现有字段：

```json
{
  "orderNo": "LL202610260001",
  "orderId": 101,
  "orderItemId": 201,
  "productId": 1,
  "rating": 5,
  "score": 5,
  "content": "苹果很脆，包装完整",
  "images": ["https://img.example.com/review/1.jpg"],
  "anonymous": false
}
```

- `orderNo` 与 `orderId` 至少提供一个；`orderItemId` 与 `productId` 至少提供一个。
- 兼容 `rating`（C 端）与 `score`（设计文档）；两者同时传入时必须一致。
- `user_id` 一律取登录态，请求体无法指定评价人。
- `ReviewOrderPortAdapter` 同时校验订单归属、订单状态为“已完成”、订单项属于该订单；任一条件不满足返回 `60003`。
- `uk_order_item` 唯一键冲突转换为 `60002`，并发重复评价也不会产生第二条记录。
- 评分范围 `1~5`，内容最长 `1000` 字符，图片最多 `9` 张且序列化后总长度不得超过数据库 `VARCHAR(1000)`。

### 2.3 查看与回复

- 公开列表仅返回 `status=1 AND deleted=0`，按创建时间倒序，分页上限 100。
- 后台列表支持 `keyword`、`productId`、`score`、`status` 筛选；手机号脱敏展示。
- 回复使用条件更新 `WHERE id=? AND deleted=0`；评价不存在返回 `60001`。
- 评价不可由运营删除，回复不改变商品、订单与资金状态。

### 2.4 错误码

| 错误码 | 场景 |
| --- | --- |
| `60001` | 评价不存在或无权访问 |
| `60002` | 同一订单项重复评价 |
| `60003` | 订单未完成、非本人订单或订单项不匹配 |

---

## 三、售后与 A 模块联调

A 已在 `develop` 提供以下实现，B 的售后模块通过 Port 自动注入：

- `OrderPortAdapter`：校验售后订单归属；条件流转售后中/已退款/驳回恢复。
- `RefundPortAdapter`：按 `refundNo` 幂等创建沙箱退款单，重复调用返回同一结果。

本周确认的行为：

- 申请售后仍只允许本人已支付、待收货或已完成订单。
- 审核通过后依次流转售后单 `PENDING → APPROVED → COMPLETED`，退款成功后订单 `AFTER_SALE → REFUNDED`。
- 退款结果未知或明确失败时，售后单停在 `REFUNDING`，可保留重试入口。
- 驳回后订单恢复到申请前状态。

---

## 四、会员管理

### 4.1 接口

| 方法 | 路径 | 权限 | 说明 |
| --- | --- | --- | --- |
| GET | `/api/v1/admin/members` | `user:member:manage` | 会员分页 |
| PUT | `/api/v1/admin/members/{id}/status` | `user:member:manage` | 启用/禁用会员 |

查询参数：`keyword`（手机号/昵称）、`status`、`pageNum`、`pageSize`。

```json
{ "status": 0 }
```

### 4.2 规则

- 列表只查询 `user_type=1`，统计订单数与累计消费。
- 订单聚合由 `MemberOrderStatsPortAdapter` 调用订单模块完成，B 不直接写订单表。
- 统计范围：状态为待发货、待收货、已完成、售后中的未删除订单。
- 会员等级：累计消费 `< ¥1000` 为青芽会员，`¥1000~¥4999.99` 为鲜叶会员，`≥ ¥5000` 为金穗会员。
- 禁用会员后调用 `TokenSessionService.revokeAll`，已签发 Token 立即失效。
- 非会员账号不能通过该接口修改状态，返回 `99001`。

---

## 五、角色与权限

### 5.1 接口

| 方法 | 路径 | 权限 | 说明 |
| --- | --- | --- | --- |
| GET | `/api/v1/admin/roles` | `system:role:manage` | 角色及权限点 |
| GET | `/api/v1/admin/permissions` | `system:role:manage` | 启用权限点目录 |
| POST | `/api/v1/admin/roles` | `system:role:manage` | 新建自定义角色 |
| PUT | `/api/v1/admin/roles/{id}/permissions` | `system:role:manage` | 覆盖分配权限点 |

新建角色请求体：

```json
{
  "name": "仓储运营",
  "code": "WAREHOUSE",
  "description": "负责库存与履约",
  "status": 1,
  "permissions": ["stock:info:view", "stock:info:adjust"]
}
```

分配权限请求体：

```json
{ "permissions": ["product:goods:manage", "stock:info:view"] }
```

### 5.2 边界规则

- `ADMIN` 始终拥有全部启用权限，即使提交空数组也不会被降权。
- `OPERATOR` 只允许后台业务权限，不能分配 C 端权限或系统管理权限。
- `MEMBER` 只允许 C 端权限，不能获得任何后台管理权限。
- 自定义角色不能获得会员、角色、账号、配置、日志等管理员专属权限。
- 未知或已停用权限点返回 `10001`，不会先删除原有权限。
- 角色权限写入成功后，在事务提交后删除 `auth:perm:{roleId}`，下一次请求立即生效。
- 内置角色编码 `MEMBER / OPERATOR / ADMIN` 不允许重复创建。

---

## 六、后台账号管理

### 6.1 接口

| 方法 | 路径 | 权限 | 说明 |
| --- | --- | --- | --- |
| GET | `/api/v1/admin/accounts` | `system:account:manage` | 后台账号分页 |
| POST | `/api/v1/admin/accounts` | `system:account:manage` | 新建后台账号 |
| PUT | `/api/v1/admin/accounts/{id}/status` | `system:account:manage` | 启用/禁用账号 |

新建账号请求体：

```json
{
  "phone": "13800000021",
  "nickname": "仓库运营",
  "role": "operator",
  "password": "abc12345"
}
```

### 6.2 安全保护

- 口令使用 BCrypt 保存，不回显明文或哈希。
- 只展示运营与管理员账号，不把会员混入后台账号列表。
- 不能禁用当前登录账号。
- 不能禁用最后一个启用中的平台管理员。
- 禁用后台账号立即撤销全部会话。
- 账号手机号重复返回 `10006`；并发插入冲突同样转换错误码。
- 当前账号体系只支持 `operator` 与 `admin` 两种后台角色。

---

## 七、数据契约与兼容说明

- `t_review.images` 为 JSON 字符串数组，序列化后长度不得超过 1000。
- `t_user` 没有最近登录时间字段，`AccountView.lastLoginAt` 当前返回 `null`。
- 请求体保留 C 端 `anonymous` 字段以兼容现有前端；冻结 DDL 没有匿名列，本期不持久化匿名标记。
- 会员等级由累计消费实时计算，不新增数据库字段。
- 订单聚合 SQL 通过 MyBatis 动态脚本验证：`user_id IN (?, ?, ...)` 可正确展开。

---

## 八、安全自查

| 检查项 | 结果 |
| --- | --- |
| 评价人由登录态决定，不接受请求体 `userId` | 通过 |
| 只能评价本人已完成订单中的订单项 | 通过 |
| 重复评价由数据库唯一键兜底 | 通过 |
| 禁用会员后全部 Token 立即撤销 | 通过 |
| 运营不能获得管理员或 C 端权限 | 通过 |
| 会员不能获得后台权限 | 通过 |
| 管理员角色不会被误降权 | 通过 |
| 不能禁用当前账号或最后一个管理员 | 通过 |
| 后台账号密码 BCrypt 存储 | 通过 |
| 后台接口均声明权限点 | 通过 |

---

## 九、验证

### 9.1 编译

- JDK：Temurin 17.0.20.1。
- 主代码：185 个 Java 源文件，全部生成 class。
- 当前沙箱中 Maven 在关闭依赖 JAR 时触发 `zipfs AccessDeniedException`；代码无 `error:`，改用同一 JDK 17、同一依赖清单完成测试编译。

### 9.2 单元测试

第二周新增测试：

- `ReviewServiceTest`：5 个。
- `MemberServiceTest`：4 个。
- `RoleAdminServiceTest`：9 个。
- `AccountServiceTest`：6 个。

B 的纯单元测试合计 46 个，覆盖第一周基线 22 个与第二周新增 24 个。

```bash
cd backend
mvn -Dtest=AccountServiceTest,AddressServiceTest,AfterSaleServiceTest,CartServiceTest,CategoryServiceTest,IdempotencyServiceTest,MemberServiceTest,ReviewServiceTest,RoleAdminServiceTest,UserServiceTest test
```

结果：`Tests run: 46, Failures: 0, Errors: 0, Skipped: 0`。

### 9.3 中间件集成

本机检查结果：

- MySQL 服务运行中，但项目默认 `root / root123456` 被实例拒绝。
- Redis 服务未启动。
- A 的 `OrderManagementTest`、`OrderPreviewTest`、`OrderServiceTest`、`PaymentServiceTest` 与 `InventoryServiceTest` 需要真实中间件，本轮未纳入通过统计。

因此本周验证结论限定为“模块单元测试通过，全链路集成待环境修复后执行”，不把环境连接失败误报为业务测试通过。

---

## 十、遗留与交接

1. 合并到 `develop` 前需由 A 评审订单聚合 SQL 与订单/退款 Port 联调边界。
2. 若后续 DDL 增加匿名评价、最近登录时间字段，可替换当前兼容行为。
3. 本机 MySQL 凭据与 Redis 启动属于 D 的环境配置项；修复后应执行全量 Spring Boot 集成测试。
4. 自定义角色已支持创建与权限分配；若后续允许后台账号绑定自定义角色，需要同步调整账号创建校验与前端角色下拉。
