# C 端真实联调证据

## 环境

- 前端主线：`main@d24750a`
- staging 公告：GitHub Issue #20
- 当前 staging：`https://greetings-catch-guidelines-retirement.trycloudflare.com`
- 联调方式：`VITE_USE_MOCK=false`，`VITE_API_PROXY` 指向 Issue #20 当前地址

## 已确认的主链路

- 订单：`ORD20261102110906226035`
- 支付：`PAY20261102110920836407`
- 售后：`AS20261102111349264013`
- 退款：`RF20261102111411690822`
- 最终订单状态：`REFUNDED`
- 最终售后状态：`COMPLETED`

## 已保存截图

- `c-review-form.png`：C 端评价页，匿名开关已隐藏
- `admin-reviews.png`：后台评价管理，3 条真实评价
- `admin-members.png`：后台会员管理，真实会员数据
- `admin-roles.png`：后台角色权限，3 个真实角色
- `admin-accounts.png`：后台账号管理，3 个真实账号
- `admin-products.png`：后台商品管理，6 个真实商品
- `admin-categories.png`：后台商品分类，4 个真实分类

## 说明

- 切换到新的 staging 后，旧管理员 Token 可能保留过期权限缓存；退出并重新登录即可恢复角色页数据。
- 后台角色、账号、评价、会员、商品、分类页面均已在新 Cloudflare 地址上完成回归并截图。
