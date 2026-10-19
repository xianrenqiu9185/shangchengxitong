-- ============================================================
-- 邻里鲜生社区超市商城系统 · 初始化数据（init_data.sql）
-- 依赖：schema.sql
-- 说明：仅用于开发/测试环境；生产环境禁止直接执行（会重置测试账号与库存基线）
-- 测试账号口令：123456（BCrypt，Spring Security 5+ 可校验）
-- ============================================================
SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ------------------------------------------------------------
-- 1. 角色
-- ------------------------------------------------------------
INSERT INTO t_role (id, name, code, remark, status) VALUES
(1, '会员',     'MEMBER',   'C 端消费者',   1),
(2, '商家运营', 'OPERATOR', '后台业务岗',   1),
(3, '平台管理员', 'ADMIN',  '后台管理岗',   1)
ON DUPLICATE KEY UPDATE name = VALUES(name), code = VALUES(code), remark = VALUES(remark), status = VALUES(status);

-- ------------------------------------------------------------
-- 2. 权限点（28 个；type 本期统一为 3=接口，前端按 module 分组）
-- ------------------------------------------------------------
INSERT INTO t_permission (id, code, name, module, type, sort, status) VALUES
(1,  'user:account:register',      '注册',             'user',       3, 10, 1),
(2,  'user:account:login',         '登录',             'user',       3, 20, 1),
(3,  'user:profile:update',        '修改个人信息',     'user',       3, 30, 1),
(4,  'address:info:manage',        '收货地址管理',     'user',       3, 40, 1),
(5,  'product:goods:view',         '浏览与搜索商品',   'product',    3, 10, 1),
(6,  'cart:item:manage',           '购物车增删改查',   'cart',       3, 10, 1),
(7,  'trade:order:create',         '下单',             'trade',      3, 10, 1),
(8,  'trade:payment:pay',          '支付（沙箱）',     'trade',      3, 20, 1),
(9,  'trade:order:query',          '查询订单',         'trade',      3, 30, 1),
(10, 'trade:order:cancel',         '取消订单',         'trade',      3, 40, 1),
(11, 'after:sale:apply',           '申请售后',         'after-sale', 3, 10, 1),
(12, 'review:goods:create',        '评价商品',         'review',     3, 10, 1),
(13, 'marketing:coupon:receive',   '领取优惠券',       'marketing',  3, 10, 1),
(14, 'product:category:manage',    '商品分类管理',     'product',    3, 20, 1),
(15, 'product:goods:manage',       '商品管理',         'product',    3, 30, 1),
(16, 'stock:info:view',            '库存查看',         'stock',      3, 10, 1),
(17, 'stock:info:adjust',          '库存调整与补货',   'stock',      3, 20, 1),
(18, 'trade:order:ship',           '订单发货',         'trade',      3, 50, 1),
(19, 'trade:order:close',          '订单关闭与异常处理', 'trade',    3, 60, 1),
(20, 'after:sale:audit',           '售后审核与退款处理', 'after-sale', 3, 20, 1),
(21, 'review:goods:reply',         '评价查看与回复',   'review',     3, 20, 1),
(22, 'marketing:coupon:manage',    '优惠券管理',       'marketing',  3, 20, 1),
(23, 'statistics:dashboard:view',  '数据看板',         'statistics', 3, 10, 1),
(24, 'user:member:manage',         '会员管理',         'user',       3, 50, 1),
(25, 'system:role:manage',         '角色与权限管理',   'system',     3, 10, 1),
(26, 'system:account:manage',      '后台账号管理',     'system',     3, 20, 1),
(27, 'system:config:manage',       '系统参数配置',     'system',     3, 30, 1),
(28, 'system:log:audit',           '日志审计',         'system',     3, 40, 1)
ON DUPLICATE KEY UPDATE name = VALUES(name), module = VALUES(module), type = VALUES(type), sort = VALUES(sort), status = VALUES(status);

-- ------------------------------------------------------------
-- 3. 角色权限
-- ------------------------------------------------------------
-- 会员：前台功能权限（注册/登录为公开接口，不分配角色）
INSERT IGNORE INTO t_role_permission (role_id, permission_id)
SELECT 1, id FROM t_permission WHERE code IN (
  'user:profile:update', 'address:info:manage', 'product:goods:view',
  'cart:item:manage', 'trade:order:create', 'trade:payment:pay',
  'trade:order:query', 'trade:order:cancel', 'after:sale:apply',
  'review:goods:create', 'marketing:coupon:receive'
);

-- 运营：商品、库存、订单、售后、评价、营销、看板
INSERT IGNORE INTO t_role_permission (role_id, permission_id)
SELECT 2, id FROM t_permission WHERE code IN (
  'product:category:manage', 'product:goods:manage', 'stock:info:view',
  'stock:info:adjust', 'trade:order:query', 'trade:order:ship',
  'trade:order:close', 'after:sale:audit', 'review:goods:reply',
  'marketing:coupon:manage', 'statistics:dashboard:view'
);

-- 管理员：全部权限（含运营业务权限 + 系统管理）
INSERT IGNORE INTO t_role_permission (role_id, permission_id)
SELECT 3, id FROM t_permission;

-- ------------------------------------------------------------
-- 4. 系统参数
-- ------------------------------------------------------------
INSERT INTO t_system_config (config_key, config_value, description) VALUES
('order.pay.timeout.minutes', '30',   '待支付超时时长（分钟）'),
('order.auto.confirm.days',   '7',    '发货后自动确认收货天数'),
('payment.sandbox.enabled',   'true', '支付沙箱开关'),
('stock.warn.threshold',      '10',   '库存预警阈值'),
('system.name',               '邻里鲜生', '系统名称')
ON DUPLICATE KEY UPDATE config_value = VALUES(config_value), description = VALUES(description);

-- ------------------------------------------------------------
-- 5. 测试账号（口令均为 123456）
-- ------------------------------------------------------------
INSERT INTO t_user (phone, password, nickname, user_type, role_id, status) VALUES
('13800000001', '$2b$10$pkpzh3uC8WsiHeKD/YCHZ.52YSacje4AcS/wmcj1VrKnFt8P5Lkqa', '平台管理员', 3, 3, 1),
('13800000002', '$2b$10$WnIi9IvfBv6QK5aru6V9KewwqyfiXvwuq7kU5RmYGy7pL/QW/ji.W', '商家运营',   2, 2, 1),
('13800000003', '$2b$10$i463s2GTX6UdOacOlbkoPeokSNpkVyd1AHly6YFqE6bPxfAfDtNcK', '邻里会员',   1, 1, 1)
ON DUPLICATE KEY UPDATE password = VALUES(password), nickname = VALUES(nickname), user_type = VALUES(user_type), role_id = VALUES(role_id), status = VALUES(status);

-- ------------------------------------------------------------
-- 6. 演示分类
-- ------------------------------------------------------------
INSERT INTO t_category (id, name, sort, status) VALUES
(1, '水果', 10, 1),
(2, '蔬菜', 20, 1),
(3, '粮油', 30, 1),
(4, '日用', 40, 1)
ON DUPLICATE KEY UPDATE name = VALUES(name), sort = VALUES(sort), status = VALUES(status);

-- ------------------------------------------------------------
-- 7. 演示商品
-- ------------------------------------------------------------
INSERT INTO t_product (id, category_id, name, spec, image, description, price, status) VALUES
(1, 1, '山东红富士苹果', '500g/份', NULL, '脆甜多汁，当日到店', 1290, 1),
(2, 1, '海南香蕉',       '1kg/份',  NULL, '自然成熟，香甜软糯', 990,  1),
(3, 2, '本地小青菜',     '500g/份', NULL, '当日采摘，新鲜直供', 350,  1),
(4, 3, '东北大米',       '5kg/袋',  NULL, '颗粒饱满，清香软糯', 3990, 1),
(5, 3, '非转基因菜籽油', '5L/桶',   NULL, '物理压榨，家庭装',   6990, 1),
(6, 4, '抽纸',           '3层100抽*6包', NULL, '原生木浆，柔韧亲肤', 1990, 1)
ON DUPLICATE KEY UPDATE category_id = VALUES(category_id), name = VALUES(name), spec = VALUES(spec), description = VALUES(description), price = VALUES(price), status = VALUES(status);

-- ------------------------------------------------------------
-- 8. 演示库存（商品 6 库存 8，可触发库存预警）
-- ------------------------------------------------------------
INSERT INTO t_inventory (product_id, total, locked, sold, version) VALUES
(1, 100, 0, 0, 0),
(2, 80,  0, 0, 0),
(3, 50,  0, 0, 0),
(4, 30,  0, 0, 0),
(5, 20,  0, 0, 0),
(6, 8,   0, 0, 0)
ON DUPLICATE KEY UPDATE total = VALUES(total), locked = VALUES(locked), sold = VALUES(sold), version = VALUES(version);

SET FOREIGN_KEY_CHECKS = 1;
