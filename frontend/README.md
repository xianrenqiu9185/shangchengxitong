# 前端工程说明

本目录包含两个独立的 Vue 3 + Vite 工程，当前默认启用本地 mock，可在后端接口尚未完成时独立演示。

## 环境

- Node.js 18 LTS 或更高版本（当前开发机为 Node 24）
- npm 9+

## C 端商城

```bash
cd frontend/linli-mall-web
npm install
npm run dev
```

访问 `http://localhost:5173`。

已覆盖：登录/注册、首页、分类、搜索、商品详情、购物车、结算、支付沙箱、订单列表/详情、发表评价、售后、优惠券、地址、个人中心。

演示账号：`13800000003` / `123456`。

## 管理后台

```bash
cd frontend/linli-admin-web
npm install
npm run dev
```

访问 `http://localhost:5174`。

已覆盖：登录、经营看板、商品、分类、库存、订单、售后、优惠券、评价、会员、角色权限、系统配置、日志审计、后台账号管理。

演示账号：

| 账号 | 手机号 | 密码 | 角色 |
| --- | --- | --- | --- |
| 管理员 | 13800000001 | 123456 | 平台管理员 |
| 运营 | 13800000002 | 123456 | 商家运营 |

运营账号不会显示会员、角色、配置与日志菜单；直接访问对应路由会跳到 403。

## Mock 与真实接口切换

两个工程默认在 `.env.development` 中设置：

```env
VITE_API_BASE_URL=/api/v1
VITE_USE_MOCK=true
```

联调时改为 `VITE_USE_MOCK=false`，并确保后端运行在 `http://localhost:8080`，Vite 会把 `/api` 代理到后端。

mock 数据保存在浏览器 `localStorage`。需要恢复初始数据时，清除下列键后刷新页面：

- `linli_mock_cart_v1`
- `linli_mock_orders_v1`
- `linli_mock_addresses_v1`
- `linli_mock_coupons_v1`
- `linli_mock_after_sales_v1`
- `linli_admin_products_v1`
- `linli_admin_categories_v1`
- `linli_admin_orders_v1`
- `linli_admin_after_sales_v1`
- `linli_admin_coupons_v1`
- `linli_admin_reviews_v1`
- `linli_admin_members_v1`
- `linli_admin_roles_v1`
- `linli_admin_configs_v1`
- `linli_admin_logs_v1`

## 构建

```bash
cd frontend/linli-mall-web && npm run build
cd ../linli-admin-web && npm run build
```

产物分别位于两个工程的 `dist/` 目录。

## 静态演示部署

不依赖后端时，使用 demo 模式构建；该模式强制启用本地 mock：

```bash
cd frontend/linli-mall-web && npm run build:demo
cd ../linli-admin-web && npm run build:demo
```

两个工程的 `public/_redirects` 会随构建进入 `dist/`，用于 Netlify 等静态平台的 SPA 深链接回退。

快速分享步骤：

1. 打开 [Netlify Drop](https://app.netlify.com/drop)。
2. 将 `frontend/linli-mall-web/dist` 拖入，得到一个 C 端链接。
3. 将 `frontend/linli-admin-web/dist` 拖入，得到另一个后台链接。
4. 把两个链接和演示账号一起发给老师或组员。

当前 demo 数据保存在访问者浏览器 `localStorage`，不会写入后端数据库。正式部署与 Docker/Nginx 仍由 D 负责。
