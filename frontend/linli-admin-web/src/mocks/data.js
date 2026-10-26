export const businessPermissions = [
  'product:category:manage',
  'product:goods:manage',
  'stock:info:view',
  'stock:info:adjust',
  'trade:order:query',
  'trade:order:ship',
  'trade:order:close',
  'after:sale:audit',
  'review:goods:reply',
  'marketing:coupon:manage',
  'statistics:dashboard:view',
]

export const adminOnlyPermissions = [
  'user:member:manage',
  'system:role:manage',
  'system:account:manage',
  'system:config:manage',
  'system:log:audit',
]

export const allPermissions = [...businessPermissions, ...adminOnlyPermissions]

export const permissionList = [
  { value: 'product:category:manage', label: '商品分类管理' },
  { value: 'product:goods:manage', label: '商品管理' },
  { value: 'stock:info:view', label: '库存查看' },
  { value: 'stock:info:adjust', label: '库存调整' },
  { value: 'trade:order:query', label: '订单查询' },
  { value: 'trade:order:ship', label: '订单发货' },
  { value: 'trade:order:close', label: '订单关闭' },
  { value: 'after:sale:audit', label: '售后审核' },
  { value: 'review:goods:reply', label: '评价回复' },
  { value: 'marketing:coupon:manage', label: '优惠券管理' },
  { value: 'statistics:dashboard:view', label: '经营看板' },
  { value: 'user:member:manage', label: '会员管理' },
  { value: 'system:role:manage', label: '角色权限管理' },
  { value: 'system:account:manage', label: '后台账号管理' },
  { value: 'system:config:manage', label: '系统配置' },
  { value: 'system:log:audit', label: '日志审计' },
]

export const users = {
  admin: {
    id: 1,
    nickname: '平台管理员',
    phone: '13800000001',
    role: 'admin',
    roleName: '平台管理员',
    avatarText: '管',
  },
  operator: {
    id: 2,
    nickname: '商家运营',
    phone: '13800000002',
    role: 'operator',
    roleName: '商家运营',
    avatarText: '运',
  },
}

export const seedCategories = [
  { id: 1, name: '时令水果', sort: 10, status: 1, productCount: 3 },
  { id: 2, name: '新鲜蔬菜', sort: 20, status: 1, productCount: 2 },
  { id: 3, name: '肉禽蛋品', sort: 30, status: 1, productCount: 1 },
  { id: 4, name: '乳品烘焙', sort: 40, status: 1, productCount: 3 },
  { id: 5, name: '米面粮油', sort: 50, status: 1, productCount: 1 },
  { id: 6, name: '海鲜水产', sort: 60, status: 1, productCount: 1 },
  { id: 7, name: '日用百货', sort: 70, status: 0, productCount: 0 },
]

export const seedProducts = [
  { id: 101, name: '红富士苹果', subtitle: '冰糖心 · 脆甜多汁', categoryId: 1, categoryName: '时令水果', price: 2990, stock: 86, sales: 1260, status: 1, image: '/images/apple.svg', updatedAt: '2026-10-19 09:20' },
  { id: 102, name: '云南蓝莓', subtitle: '大果粉霜 · 酸甜爆汁', categoryId: 1, categoryName: '时令水果', price: 4590, stock: 42, sales: 680, status: 1, image: '/images/blueberry.svg', updatedAt: '2026-10-19 09:12' },
  { id: 103, name: '每日鲜牛奶', subtitle: '低温巴氏杀菌 · 950ml', categoryId: 4, categoryName: '乳品烘焙', price: 1290, stock: 120, sales: 2380, status: 1, image: '/images/milk.svg', updatedAt: '2026-10-19 08:55' },
  { id: 104, name: '谷物鲜鸡蛋', subtitle: '蛋黄饱满 · 30枚装', categoryId: 3, categoryName: '肉禽蛋品', price: 2590, stock: 64, sales: 1540, status: 1, image: '/images/eggs.svg', updatedAt: '2026-10-18 18:30' },
  { id: 105, name: '有机小青菜', subtitle: '清晨采摘 · 500g', categoryId: 2, categoryName: '新鲜蔬菜', price: 690, stock: 38, sales: 920, status: 1, image: '/images/greens.svg', updatedAt: '2026-10-18 17:20' },
  { id: 106, name: '五常大米', subtitle: '当季新米 · 5kg', categoryId: 5, categoryName: '米面粮油', price: 5990, stock: 25, sales: 430, status: 1, image: '/images/rice.svg', updatedAt: '2026-10-18 15:08' },
  { id: 107, name: '赣南脐橙', subtitle: '现摘现发 · 5斤装', categoryId: 1, categoryName: '时令水果', price: 2490, stock: 56, sales: 1100, status: 1, image: '/images/orange.svg', updatedAt: '2026-10-18 13:45' },
  { id: 108, name: '全麦吐司', subtitle: '低糖配方 · 400g', categoryId: 4, categoryName: '乳品烘焙', price: 990, stock: 48, sales: 760, status: 0, image: '/images/bread.svg', updatedAt: '2026-10-18 11:22' },
  { id: 109, name: '鲜活基围虾', subtitle: '活虾现捞 · 500g', categoryId: 6, categoryName: '海鲜水产', price: 4290, stock: 18, sales: 310, status: 1, image: '/images/shrimp.svg', updatedAt: '2026-10-18 10:05' },
  { id: 110, name: '沙瓤番茄', subtitle: '自然成熟 · 1kg', categoryId: 2, categoryName: '新鲜蔬菜', price: 1190, stock: 72, sales: 1450, status: 1, image: '/images/tomato.svg', updatedAt: '2026-10-17 19:40' },
]

function orderItem(productId, quantity) {
  const product = seedProducts.find((item) => item.id === productId)
  return {
    productId,
    name: product.name,
    image: product.image,
    price: product.price,
    unit: product.subtitle,
    quantity,
  }
}

export const seedOrders = [
  { orderNo: 'LL202610190001', customer: '林女士', phone: '13800000003', status: 'PENDING_SHIPMENT', deliveryType: 'DELIVERY', payAmount: 5070, goodsAmount: 5570, deliveryFee: 0, discountAmount: 500, createdAt: '2026-10-19 09:30', address: '上海市浦东新区邻里社区 3 号楼 1202 室', items: [orderItem(101, 1), orderItem(103, 2)] },
  { orderNo: 'LL202610190002', customer: '周先生', phone: '13800000011', status: 'PENDING_PAYMENT', deliveryType: 'DELIVERY', payAmount: 4590, goodsAmount: 4290, deliveryFee: 300, discountAmount: 0, createdAt: '2026-10-19 09:18', address: '上海市浦东新区邻里社区 6 号楼 801 室', items: [orderItem(109, 1)] },
  { orderNo: 'LL202610190003', customer: '王女士', phone: '13800000012', status: 'PENDING_RECEIPT', deliveryType: 'PICKUP', payAmount: 3580, goodsAmount: 3580, deliveryFee: 0, discountAmount: 0, createdAt: '2026-10-19 08:42', address: '邻里鲜生超市前台（自提点）', items: [orderItem(104, 1), orderItem(108, 1)] },
  { orderNo: 'LL202610180004', customer: '陈先生', phone: '13800000013', status: 'COMPLETED', deliveryType: 'DELIVERY', payAmount: 4590, goodsAmount: 4590, deliveryFee: 0, discountAmount: 0, createdAt: '2026-10-18 17:20', address: '上海市浦东新区邻里社区 2 号楼 502 室', items: [orderItem(102, 1)] },
  { orderNo: 'LL202610180005', customer: '赵女士', phone: '13800000014', status: 'CANCELLED', deliveryType: 'DELIVERY', payAmount: 2490, goodsAmount: 2490, deliveryFee: 0, discountAmount: 0, createdAt: '2026-10-18 15:10', address: '上海市浦东新区邻里社区 5 号楼 302 室', items: [orderItem(107, 1)] },
  { orderNo: 'LL202610170006', customer: '孙先生', phone: '13800000015', status: 'COMPLETED', deliveryType: 'PICKUP', payAmount: 690, goodsAmount: 690, deliveryFee: 0, discountAmount: 0, createdAt: '2026-10-17 11:32', address: '邻里鲜生超市前台（自提点）', items: [orderItem(105, 1)] },
]

export const seedAfterSales = [
  { saleNo: 'AS202610190001', orderNo: 'LL202610180004', customer: '陈先生', type: 'REFUND', reason: '蓝莓有部分压坏', status: 'PENDING', amount: 4590, createdAt: '2026-10-19 09:05' },
  { saleNo: 'AS202610180002', orderNo: 'LL202610170006', customer: '孙先生', type: 'RETURN', reason: '商品与描述不符', status: 'APPROVED', amount: 690, createdAt: '2026-10-18 14:20' },
  { saleNo: 'AS202610170003', orderNo: 'LL202610160007', customer: '刘女士', type: 'REFUND', reason: '少件', status: 'REJECTED', amount: 1290, createdAt: '2026-10-17 10:08' },
]

export const seedCoupons = [
  { id: 1, name: '新人满 39 减 5', threshold: 3900, amount: 500, validUntil: '2026-11-30', status: 1, received: 126 },
  { id: 2, name: '生鲜满 69 减 10', threshold: 6900, amount: 1000, validUntil: '2026-11-15', status: 1, received: 84 },
  { id: 3, name: '会员专享满 99 减 15', threshold: 9900, amount: 1500, validUntil: '2026-11-01', status: 0, received: 32 },
]

export const seedReviews = [
  { id: 1, productName: '红富士苹果', user: '林女士', rating: 5, content: '苹果很脆，个头均匀，家里人都喜欢。', reply: '感谢支持，欢迎下次继续选购。', createdAt: '2026-10-18 19:20' },
  { id: 2, productName: '每日鲜牛奶', user: '王女士', rating: 4, content: '奶味很浓，日期新鲜，早餐很合适。', reply: '', createdAt: '2026-10-18 08:12' },
  { id: 3, productName: '谷物鲜鸡蛋', user: '周先生', rating: 5, content: '没有破损，蛋黄颜色很好。', reply: '', createdAt: '2026-10-17 16:40' },
  { id: 4, productName: '鲜活基围虾', user: '陈先生', rating: 4, content: '个头可以，送到时还是活的。', reply: '', createdAt: '2026-10-17 12:05' },
]

export const seedMembers = [
  { id: 3, phone: '13800000003', nickname: '林女士', level: '青芽会员', status: 1, orderCount: 12, totalSpent: 56800, createdAt: '2026-09-12' },
  { id: 11, phone: '13800000011', nickname: '周先生', level: '青芽会员', status: 1, orderCount: 5, totalSpent: 18600, createdAt: '2026-09-20' },
  { id: 12, phone: '13800000012', nickname: '王女士', level: '青芽会员', status: 1, orderCount: 8, totalSpent: 32400, createdAt: '2026-09-24' },
  { id: 13, phone: '13800000013', nickname: '陈先生', level: '鲜叶会员', status: 1, orderCount: 21, totalSpent: 126800, createdAt: '2026-08-30' },
  { id: 14, phone: '13800000014', nickname: '赵女士', level: '青芽会员', status: 0, orderCount: 2, totalSpent: 5600, createdAt: '2026-10-02' },
]

export const seedRoles = [
  { id: 1, name: '平台管理员', description: '拥有全部业务与系统管理权限', permissionCount: 16, permissions: allPermissions, updatedAt: '2026-10-18 10:00' },
  { id: 2, name: '商家运营', description: '负责商品、库存、订单、售后与营销', permissionCount: businessPermissions.length, permissions: businessPermissions, updatedAt: '2026-10-18 10:00' },
]

export const seedConfigs = [
  { key: 'order.paymentTimeoutMinutes', value: '30', description: '待支付订单超时关单时长（分钟）', updatedAt: '2026-10-18 09:00' },
  { key: 'order.autoConfirmDays', value: '7', description: '发货后自动确认收货天数', updatedAt: '2026-10-18 09:00' },
  { key: 'delivery.freeShippingThreshold', value: '3990', description: '免配送费门槛（分）', updatedAt: '2026-10-18 09:00' },
  { key: 'delivery.fee', value: '300', description: '到家配送费（分）', updatedAt: '2026-10-18 09:00' },
  { key: 'pay.mockEnabled', value: 'true', description: '是否启用支付沙箱', updatedAt: '2026-10-18 09:00' },
]

export const seedLogs = [
  { id: 1, time: '2026-10-19 09:32', user: '商家运营', action: '确认订单发货', target: 'LL202610190001', traceId: 'a13f9c7b' },
  { id: 2, time: '2026-10-19 09:21', user: '平台管理员', action: '调整商品库存', target: '红富士苹果 +20', traceId: 'b27e1d4a' },
  { id: 3, time: '2026-10-19 09:08', user: '商家运营', action: '审核售后申请', target: 'AS202610190001', traceId: 'c91a2f6e' },
  { id: 4, time: '2026-10-19 08:55', user: '平台管理员', action: '修改系统配置', target: 'order.paymentTimeoutMinutes', traceId: 'd44b8e2f' },
  { id: 5, time: '2026-10-18 18:30', user: '商家运营', action: '商品上架', target: '赣南脐橙', traceId: 'e18c7a3d' },
  { id: 6, time: '2026-10-18 15:10', user: '商家运营', action: '关闭异常订单', target: 'LL202610180005', traceId: 'f72d5b1c' },
]
