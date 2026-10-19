import {
  adminOnlyPermissions,
  allPermissions,
  businessPermissions,
  permissionList,
  seedAfterSales,
  seedCategories,
  seedConfigs,
  seedCoupons,
  seedLogs,
  seedMembers,
  seedOrders,
  seedProducts,
  seedReviews,
  seedRoles,
  users,
} from './data'
import { getRole } from '@/utils/auth'

const KEYS = {
  products: 'linli_admin_products_v1',
  categories: 'linli_admin_categories_v1',
  orders: 'linli_admin_orders_v1',
  afterSales: 'linli_admin_after_sales_v1',
  coupons: 'linli_admin_coupons_v1',
  reviews: 'linli_admin_reviews_v1',
  members: 'linli_admin_members_v1',
  roles: 'linli_admin_roles_v1',
  configs: 'linli_admin_configs_v1',
  logs: 'linli_admin_logs_v1',
}

const delay = (ms = 220) => new Promise((resolve) => setTimeout(resolve, ms))
const traceId = () => Math.random().toString(16).slice(2, 10)

function read(key, fallback) {
  try {
    const value = localStorage.getItem(key)
    return value ? JSON.parse(value) : fallback
  } catch {
    return fallback
  }
}

function write(key, value) {
  localStorage.setItem(key, JSON.stringify(value))
}

function seed(key, value) {
  const current = read(key, null)
  if (current) return current
  write(key, value)
  return value
}

const ok = (data) => ({ code: 0, message: 'OK', data, traceId: traceId(), timestamp: Date.now() })
const fail = (code, message) => ({ code, message, data: null, traceId: traceId(), timestamp: Date.now() })

function parseBody(config) {
  if (!config.data) return {}
  if (typeof config.data === 'string') {
    try {
      return JSON.parse(config.data)
    } catch {
      return {}
    }
  }
  return config.data
}

function parseUrl(config) {
  const raw = `${config.baseURL || ''}${config.url || ''}`
  const url = new URL(raw, 'http://mock.local')
  return {
    path: url.pathname.replace(/^\/api\/v1/, '') || '/',
    query: Object.fromEntries(url.searchParams.entries()),
  }
}

function listData(key, fallback) {
  return seed(key, fallback)
}

function productList() {
  return listData(KEYS.products, seedProducts)
}

function orderList() {
  return listData(KEYS.orders, seedOrders)
}

function afterSaleList() {
  return listData(KEYS.afterSales, seedAfterSales)
}

function permissionFor(role) {
  return role === 'admin' ? allPermissions : businessPermissions
}

function profileFor(role) {
  return role === 'admin' ? users.admin : users.operator
}

function resourceArray(path) {
  if (path.startsWith('/admin/categories')) return { key: KEYS.categories, seed: seedCategories, idKey: 'id' }
  if (path.startsWith('/admin/coupons')) return { key: KEYS.coupons, seed: seedCoupons, idKey: 'id' }
  if (path.startsWith('/admin/reviews')) return { key: KEYS.reviews, seed: seedReviews, idKey: 'id' }
  if (path.startsWith('/admin/members')) return { key: KEYS.members, seed: seedMembers, idKey: 'id' }
  if (path.startsWith('/admin/roles')) return { key: KEYS.roles, seed: seedRoles, idKey: 'id' }
  if (path.startsWith('/admin/configs')) return { key: KEYS.configs, seed: seedConfigs, idKey: 'key' }
  if (path.startsWith('/admin/logs')) return { key: KEYS.logs, seed: seedLogs, idKey: 'id' }
  return null
}

async function handleMock(config) {
  await delay()
  const method = String(config.method || 'get').toLowerCase()
  const { path, query } = parseUrl(config)
  const body = parseBody(config)
  const role = getRole()

  if (path === '/auth/login' && method === 'post') {
    if (!body.phone || !body.password) return fail(10007, '请输入账号和密码')
    if (String(body.password) !== '123456') return fail(10007, '账号或密码错误')
    if (String(body.phone) === '13800000002') {
      return ok({ token: `admin-token-operator-${Date.now()}`, user: users.operator, permissions: businessPermissions })
    }
    if (String(body.phone) === '13800000001') {
      return ok({ token: `admin-token-admin-${Date.now()}`, user: users.admin, permissions: allPermissions })
    }
    return fail(10007, '该账号不是后台账号')
  }

  if (path === '/auth/logout' && method === 'post') return ok(null)

  if (path === '/users/me' && method === 'get') {
    return ok({ user: profileFor(role), permissions: permissionFor(role) })
  }

  if (path === '/admin/permissions' && method === 'get') return ok(permissionList)

  if (path === '/admin/statistics/dashboard' && method === 'get') {
    const orders = orderList()
    const paid = orders.filter((order) => !['CANCELLED'].includes(order.status))
    const today = orders.filter((order) => String(order.createdAt).startsWith('2026-10-19'))
    return ok({
      todayOrders: today.length,
      todaySales: today.reduce((sum, order) => sum + order.payAmount, 0),
      pendingShipment: orders.filter((order) => order.status === 'PENDING_SHIPMENT').length,
      lowStock: productList().filter((product) => product.stock <= 30).length,
      orderTrend: [
        { date: '10-13', orders: 36, sales: 218000 },
        { date: '10-14', orders: 42, sales: 256000 },
        { date: '10-15', orders: 38, sales: 231000 },
        { date: '10-16', orders: 55, sales: 328000 },
        { date: '10-17', orders: 61, sales: 364000 },
        { date: '10-18', orders: 58, sales: 341000 },
        { date: '10-19', orders: today.length, sales: today.reduce((sum, order) => sum + order.payAmount, 0) },
      ],
      lowStockProducts: productList().filter((product) => product.stock <= 30).slice(0, 5),
      recentOrders: orders.slice(0, 5),
      paidAmount: paid.reduce((sum, order) => sum + order.payAmount, 0),
    })
  }

  if (path === '/admin/products' && method === 'get') {
    let list = productList()
    if (query.keyword) {
      const keyword = String(query.keyword)
      list = list.filter((item) => `${item.name}${item.categoryName}`.includes(keyword))
    }
    if (query.status !== undefined && query.status !== '') list = list.filter((item) => String(item.status) === String(query.status))
    const pageNum = Number(query.pageNum || 1)
    const pageSize = Number(query.pageSize || 20)
    return ok({ list: list.slice((pageNum - 1) * pageSize, pageNum * pageSize), total: list.length, pageNum, pageSize })
  }

  if (path === '/admin/products' && method === 'post') {
    const list = productList()
    const item = { ...body, id: Date.now(), sales: 0, updatedAt: new Date().toLocaleString('zh-CN', { hour12: false }) }
    write(KEYS.products, [item, ...list])
    return ok(item)
  }

  const productMatch = path.match(/^\/admin\/products\/(\d+)$/)
  if (productMatch) {
    const id = Number(productMatch[1])
    let list = productList()
    if (method === 'put') {
      list = list.map((item) => (item.id === id ? { ...item, ...body, id, updatedAt: new Date().toLocaleString('zh-CN', { hour12: false }) } : item))
      write(KEYS.products, list)
      return ok(list.find((item) => item.id === id))
    }
    if (method === 'delete') {
      write(KEYS.products, list.filter((item) => item.id !== id))
      return ok(null)
    }
  }

  const productStatusMatch = path.match(/^\/admin\/products\/(\d+)\/status$/)
  if (productStatusMatch && method === 'put') {
    const id = Number(productStatusMatch[1])
    const list = productList().map((item) => (item.id === id ? { ...item, status: Number(body.status) } : item))
    write(KEYS.products, list)
    return ok(list.find((item) => item.id === id))
  }

  if (path === '/admin/orders' && method === 'get') {
    let list = orderList()
    if (query.status) list = list.filter((order) => order.status === query.status)
    if (query.keyword) list = list.filter((order) => `${order.orderNo}${order.customer}${order.phone}`.includes(String(query.keyword)))
    return ok({ list, total: list.length, pageNum: 1, pageSize: 50 })
  }

  const orderMatch = path.match(/^\/admin\/orders\/([^/]+)$/)
  if (orderMatch && method === 'get') {
    const order = orderList().find((item) => item.orderNo === orderMatch[1])
    return order ? ok(order) : fail(99001, '订单不存在')
  }

  const shipMatch = path.match(/^\/admin\/orders\/([^/]+)\/ship$/)
  if (shipMatch && method === 'post') {
    const list = orderList()
    const order = list.find((item) => item.orderNo === shipMatch[1])
    if (!order) return fail(99001, '订单不存在')
    if (order.status !== 'PENDING_SHIPMENT') return fail(30002, '当前订单状态不允许发货')
    order.status = 'PENDING_RECEIPT'
    write(KEYS.orders, list)
    return ok(order)
  }

  const closeMatch = path.match(/^\/admin\/orders\/([^/]+)\/close$/)
  if (closeMatch && method === 'post') {
    const list = orderList()
    const order = list.find((item) => item.orderNo === closeMatch[1])
    if (!order) return fail(99001, '订单不存在')
    if (['COMPLETED', 'CANCELLED'].includes(order.status)) return fail(30002, '当前订单状态不允许关闭')
    order.status = 'CANCELLED'
    write(KEYS.orders, list)
    return ok(order)
  }

  if (path === '/admin/after-sales' && method === 'get') {
    let list = afterSaleList()
    if (query.status) list = list.filter((item) => item.status === query.status)
    return ok({ list, total: list.length, pageNum: 1, pageSize: 50 })
  }

  const auditMatch = path.match(/^\/admin\/after-sales\/([^/]+)\/audit$/)
  if (auditMatch && method === 'post') {
    const list = afterSaleList()
    const record = list.find((item) => item.saleNo === auditMatch[1])
    if (!record) return fail(99001, '售后单不存在')
    record.status = body.approved ? 'APPROVED' : 'REJECTED'
    record.auditRemark = body.remark || ''
    write(KEYS.afterSales, list)
    return ok(record)
  }

  if (path === '/admin/inventories' && method === 'get') {
    const list = productList().map((product, index) => ({
      productId: product.id,
      productName: product.name,
      image: product.image,
      available: product.stock,
      locked: index % 4 === 0 ? 6 : index % 3 === 0 ? 3 : 0,
      sold: product.sales,
      threshold: product.stock <= 25 ? 30 : 20,
      updatedAt: product.updatedAt,
    }))
    return ok({ list, total: list.length, pageNum: 1, pageSize: 50 })
  }

  const inventoryMatch = path.match(/^\/admin\/inventories\/(\d+)$/)
  if (inventoryMatch && method === 'put') {
    const id = Number(inventoryMatch[1])
    const list = productList().map((item) => (item.id === id ? { ...item, stock: Number(body.available), updatedAt: new Date().toLocaleString('zh-CN', { hour12: false }) } : item))
    write(KEYS.products, list)
    return ok({ productId: id, available: Number(body.available) })
  }

  const resource = resourceArray(path)
  if (resource && method === 'get') {
    let list = listData(resource.key, resource.seed)
    if (query.keyword) {
      const keyword = String(query.keyword)
      list = list.filter((item) => JSON.stringify(item).includes(keyword))
    }
    return ok({ list, total: list.length, pageNum: 1, pageSize: 50 })
  }

  if (path === '/admin/reviews' && method === 'get') {
    return ok({ list: listData(KEYS.reviews, seedReviews), total: seedReviews.length, pageNum: 1, pageSize: 50 })
  }

  const reviewReplyMatch = path.match(/^\/admin\/reviews\/(\d+)\/reply$/)
  if (reviewReplyMatch && method === 'post') {
    const id = Number(reviewReplyMatch[1])
    const list = listData(KEYS.reviews, seedReviews).map((item) => (item.id === id ? { ...item, reply: body.reply } : item))
    write(KEYS.reviews, list)
    return ok(list.find((item) => item.id === id))
  }

  const memberStatusMatch = path.match(/^\/admin\/members\/(\d+)\/status$/)
  if (memberStatusMatch && method === 'put') {
    const id = Number(memberStatusMatch[1])
    const list = listData(KEYS.members, seedMembers).map((item) => (item.id === id ? { ...item, status: Number(body.status) } : item))
    write(KEYS.members, list)
    return ok(list.find((item) => item.id === id))
  }

  const rolePermissionMatch = path.match(/^\/admin\/roles\/(\d+)\/permissions$/)
  if (rolePermissionMatch && method === 'put') {
    const id = Number(rolePermissionMatch[1])
    const list = listData(KEYS.roles, seedRoles).map((item) => (item.id === id ? { ...item, permissions: body.permissions || [], permissionCount: (body.permissions || []).length } : item))
    write(KEYS.roles, list)
    return ok(list.find((item) => item.id === id))
  }

  const configMatch = path.match(/^\/admin\/configs\/([^/]+)$/)
  if (configMatch && method === 'put') {
    const key = decodeURIComponent(configMatch[1])
    const list = listData(KEYS.configs, seedConfigs).map((item) => (item.key === key ? { ...item, value: String(body.value), updatedAt: new Date().toLocaleString('zh-CN', { hour12: false }) } : item))
    write(KEYS.configs, list)
    return ok(list.find((item) => item.key === key))
  }

  if (resource && method === 'post') {
    const list = listData(resource.key, resource.seed)
    const item = { ...body, [resource.idKey]: resource.idKey === 'id' ? Date.now() : body[resource.idKey] }
    write(resource.key, [item, ...list])
    return ok(item)
  }

  if (resource && method === 'put') {
    const parts = path.split('/')
    const id = decodeURIComponent(parts[parts.length - 1])
    const list = listData(resource.key, resource.seed).map((item) => (String(item[resource.idKey]) === String(id) ? { ...item, ...body, [resource.idKey]: item[resource.idKey] } : item))
    write(resource.key, list)
    return ok(list.find((item) => String(item[resource.idKey]) === String(id)))
  }

  if (resource && method === 'delete') {
    const parts = path.split('/')
    const id = decodeURIComponent(parts[parts.length - 1])
    write(resource.key, listData(resource.key, resource.seed).filter((item) => String(item[resource.idKey]) !== String(id)))
    return ok(null)
  }

  return fail(99001, '接口不存在或无权访问')
}

export async function mockAdapter(config) {
  const payload = await handleMock(config)
  return {
    data: payload,
    status: 200,
    statusText: 'OK',
    headers: {},
    config,
    request: {},
  }
}
