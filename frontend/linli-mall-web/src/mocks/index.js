import { coupons, categories, memberPermissions, mockProfile, products, seedAddresses, seedOrders, seedReviews } from './data'

const CART_KEY = 'linli_mock_cart_v1'
const ORDER_KEY = 'linli_mock_orders_v1'
const ADDRESS_KEY = 'linli_mock_addresses_v1'
const COUPON_KEY = 'linli_mock_coupons_v1'
const AFTER_SALE_KEY = 'linli_mock_after_sales_v1'
const REVIEW_KEY = 'linli_mock_reviews_v1'

const delay = (ms = 180) => new Promise((resolve) => setTimeout(resolve, ms))
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

function ok(data) {
  return { code: 0, message: 'OK', data, traceId: traceId(), timestamp: Date.now() }
}

function fail(code, message) {
  return { code, message, data: null, traceId: traceId(), timestamp: Date.now() }
}

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
  const query = Object.fromEntries(url.searchParams.entries())
  for (const [key, value] of Object.entries(config.params || {})) {
    if (value !== undefined && value !== null && value !== '') query[key] = String(value)
  }
  return {
    path: url.pathname.replace(/^\/api\/v1/, '') || '/',
    query,
  }
}

function cartFromStorage() {
  return read(CART_KEY, [])
}

function enrichCart(items) {
  return items
    .map((item) => {
      const product = products.find((p) => p.id === Number(item.productId))
      if (!product) return null
      return { ...item, product, subtotal: product.price * item.quantity }
    })
    .filter(Boolean)
}

function cartPayload() {
  const items = enrichCart(cartFromStorage())
  const selected = items.filter((item) => item.checked)
  return {
    items,
    count: items.reduce((sum, item) => sum + item.quantity, 0),
    selectedCount: selected.reduce((sum, item) => sum + item.quantity, 0),
    total: selected.reduce((sum, item) => sum + item.subtotal, 0),
  }
}

function ordersFromStorage() {
  const stored = read(ORDER_KEY, null)
  if (stored) return stored
  write(ORDER_KEY, seedOrders)
  return seedOrders
}

function addressesFromStorage() {
  const stored = read(ADDRESS_KEY, null)
  if (stored) return stored
  write(ADDRESS_KEY, seedAddresses)
  return seedAddresses
}

function reviewsFromStorage() {
  const stored = read(REVIEW_KEY, null)
  if (stored) return stored
  write(REVIEW_KEY, seedReviews)
  return seedReviews
}

function orderNo() {
  const now = new Date()
  const pad = (n) => String(n).padStart(2, '0')
  return `LL${now.getFullYear()}${pad(now.getMonth() + 1)}${pad(now.getDate())}${pad(now.getHours())}${pad(now.getMinutes())}${pad(now.getSeconds())}`
}

function deliveryFeeFor(goodsAmount) {
  return goodsAmount >= 3990 ? 0 : 300
}

async function handleMock(config) {
  await delay()
  const method = String(config.method || 'get').toLowerCase()
  const { path, query } = parseUrl(config)
  const body = parseBody(config)

  if (path === '/categories' && method === 'get') return ok(categories)

  if (path === '/products' && method === 'get') {
    let list = products.filter((item) => item.stock > 0)
    if (query.categoryId && Number(query.categoryId) !== 0) {
      list = list.filter((item) => item.categoryId === Number(query.categoryId))
    }
    if (query.keyword) {
      const keyword = String(query.keyword).trim()
      list = list.filter((item) => `${item.name}${item.subtitle}${item.tags.join('')}`.includes(keyword))
    }
    if (query.sort === 'priceAsc') list = [...list].sort((a, b) => a.price - b.price)
    if (query.sort === 'salesDesc') list = [...list].sort((a, b) => b.sales - a.sales)
    const pageNum = Number(query.pageNum || 1)
    const pageSize = Number(query.pageSize || 20)
    return ok({ list: list.slice((pageNum - 1) * pageSize, pageNum * pageSize), total: list.length, pageNum, pageSize })
  }

  const productMatch = path.match(/^\/products\/(\d+)$/)
  if (productMatch && method === 'get') {
    const product = products.find((item) => item.id === Number(productMatch[1]))
    return product ? ok(product) : fail(20004, '商品不存在')
  }

  const reviewsMatch = path.match(/^\/products\/(\d+)\/reviews$/)
  if (reviewsMatch && method === 'get') {
    const list = reviewsFromStorage().filter((item) => item.productId === Number(reviewsMatch[1]))
    return ok({ list, total: list.length, pageNum: 1, pageSize: 20 })
  }

  if (path === '/auth/login' && method === 'post') {
    if (!body.phone || !body.password) return fail(10007, '请输入手机号和密码')
    if (String(body.password) !== '123456') return fail(10007, '手机号或密码错误')
    const user = { ...mockProfile, phone: String(body.phone) }
    return ok({ token: `mock-token-${body.phone}-${Date.now()}`, user, permissions: memberPermissions })
  }

  if (path === '/auth/register' && method === 'post') {
    if (!body.phone || !body.password) return fail(10007, '请完整填写注册信息')
    const user = { ...mockProfile, nickname: body.nickname || '邻里新会员', phone: String(body.phone), points: 0 }
    return ok({ token: `mock-token-${body.phone}-${Date.now()}`, user, permissions: memberPermissions })
  }

  if (path === '/auth/logout' && method === 'post') return ok(null)

  if (path === '/reviews' && method === 'post') {
    const order = ordersFromStorage().find((item) => item.orderNo === body.orderNo)
    if (!order) return fail(99001, '订单不存在或无权访问')
    if (order.status !== 'COMPLETED') return fail(60003, '订单完成后才可以评价')
    const item = order.items.find((orderItem) => orderItem.productId === Number(body.productId))
    if (!item) return fail(60003, '该商品不在当前订单中')
    const content = String(body.content || '').trim()
    if (!content) return fail(60003, '评价内容不能为空')
    const list = reviewsFromStorage()
    if (list.some((review) => review.orderNo === body.orderNo && Number(review.productId) === Number(body.productId))) {
      return fail(60002, '该商品已评价')
    }
    const review = {
      id: Date.now(),
      productId: Number(body.productId),
      orderNo: body.orderNo,
      user: body.anonymous ? '匿名邻居' : mockProfile.nickname,
      rating: Number(body.rating || 5),
      content,
      createdAt: new Date().toLocaleString('zh-CN', { hour12: false }),
    }
    write(REVIEW_KEY, [review, ...list])
    return ok(review)
  }

  if (path === '/users/me' && method === 'get') {
    return ok({ user: mockProfile, permissions: memberPermissions })
  }

  if (path === '/users/me' && method === 'put') {
    return ok({ user: { ...mockProfile, ...body }, permissions: memberPermissions })
  }

  if (path === '/users/me/addresses' && method === 'get') return ok(addressesFromStorage())

  if (path === '/users/me/addresses' && method === 'post') {
    const list = addressesFromStorage()
    const item = { ...body, id: Date.now() }
    if (item.isDefault) list.forEach((address) => { address.isDefault = false })
    write(ADDRESS_KEY, [...list, item])
    return ok(item)
  }

  const addressMatch = path.match(/^\/users\/me\/addresses\/(\d+)$/)
  if (addressMatch) {
    let list = addressesFromStorage()
    const id = Number(addressMatch[1])
    if (method === 'put') {
      if (body.isDefault) list.forEach((address) => { address.isDefault = false })
      list = list.map((address) => (address.id === id ? { ...address, ...body, id } : address))
      write(ADDRESS_KEY, list)
      return ok(list.find((address) => address.id === id))
    }
    if (method === 'delete') {
      write(ADDRESS_KEY, list.filter((address) => address.id !== id))
      return ok(null)
    }
  }

  if (path === '/cart' && method === 'get') return ok(cartPayload())

  if (path === '/cart/items' && method === 'post') {
    const list = cartFromStorage()
    const productId = Number(body.productId)
    const quantity = Number(body.quantity || 1)
    const product = products.find((item) => item.id === productId)
    if (!product) return fail(20004, '商品不存在')
    if (quantity < 1) return fail(20010, '数量非法')
    const existing = list.find((item) => item.productId === productId)
    if (existing) {
      existing.quantity = Math.min(existing.quantity + quantity, product.stock)
    } else {
      list.push({ id: Date.now(), productId, quantity, checked: true })
    }
    write(CART_KEY, list)
    return ok(cartPayload())
  }

  if (path === '/cart/items' && method === 'delete') {
    write(CART_KEY, [])
    return ok(cartPayload())
  }

  const cartItemMatch = path.match(/^\/cart\/items\/(\d+)$/)
  if (cartItemMatch) {
    let list = cartFromStorage()
    const id = Number(cartItemMatch[1])
    if (method === 'put') {
      list = list.map((item) => (item.id === id ? { ...item, ...body, id } : item))
    } else if (method === 'delete') {
      list = list.filter((item) => item.id !== id)
    }
    write(CART_KEY, list)
    return ok(cartPayload())
  }

  if (path === '/coupons' && method === 'get') {
    const received = read(COUPON_KEY, [])
    return ok(coupons.map((coupon) => ({ ...coupon, received: received.includes(coupon.id) })))
  }

  const couponReceiveMatch = path.match(/^\/coupons\/(\d+)\/receive$/)
  if (couponReceiveMatch && method === 'post') {
    const id = Number(couponReceiveMatch[1])
    const received = read(COUPON_KEY, [])
    if (received.includes(id)) return fail(50004, '该优惠券已领取')
    write(COUPON_KEY, [...received, id])
    return ok({ id })
  }

  if (path === '/users/me/coupons' && method === 'get') {
    const received = read(COUPON_KEY, [])
    return ok(coupons.filter((coupon) => received.includes(coupon.id)))
  }

  if (path === '/orders/preview' && method === 'post') {
    const items = enrichCart(cartFromStorage()).filter((item) => item.checked)
    const goodsAmount = items.reduce((sum, item) => sum + item.subtotal, 0)
    const deliveryFee = body.deliveryType === 'PICKUP' ? 0 : deliveryFeeFor(goodsAmount)
    const coupon = coupons.find((item) => item.id === Number(body.couponId))
    const discountAmount = coupon && goodsAmount >= coupon.threshold ? coupon.amount : 0
    return ok({ items, goodsAmount, deliveryFee, discountAmount, payAmount: goodsAmount + deliveryFee - discountAmount })
  }

  if (path === '/orders' && method === 'post') {
    const items = enrichCart(cartFromStorage()).filter((item) => item.checked)
    if (!items.length) return fail(30004, '请选择结算商品')
    const goodsAmount = items.reduce((sum, item) => sum + item.subtotal, 0)
    const deliveryFee = body.deliveryType === 'PICKUP' ? 0 : deliveryFeeFor(goodsAmount)
    const coupon = coupons.find((item) => item.id === Number(body.couponId))
    const discountAmount = coupon && goodsAmount >= coupon.threshold ? coupon.amount : 0
    const address = addressesFromStorage().find((item) => item.id === Number(body.addressId)) || addressesFromStorage()[0]
    const order = {
      orderNo: orderNo(),
      status: 'PENDING_PAYMENT',
      deliveryType: body.deliveryType || 'DELIVERY',
      items: items.map((item) => ({
        productId: item.product.id,
        name: item.product.name,
        image: item.product.image,
        price: item.product.price,
        unit: item.product.unit,
        quantity: item.quantity,
      })),
      goodsAmount,
      deliveryFee,
      discountAmount,
      payAmount: goodsAmount + deliveryFee - discountAmount,
      address,
      createdAt: new Date().toLocaleString('zh-CN', { hour12: false }),
      pickupCode: body.deliveryType === 'PICKUP' ? `A${Math.floor(100 + Math.random() * 900)}` : '',
      remark: body.remark || '',
    }
    const list = ordersFromStorage()
    write(ORDER_KEY, [order, ...list])
    const cart = cartFromStorage().filter((item) => !item.checked)
    write(CART_KEY, cart)
    return ok(order)
  }

  if (path === '/orders' && method === 'get') {
    let list = ordersFromStorage()
    if (query.status) list = list.filter((order) => order.status === query.status)
    return ok({ list, total: list.length, pageNum: 1, pageSize: 20 })
  }

  const orderMatch = path.match(/^\/orders\/([^/]+)$/)
  if (orderMatch && method === 'get') {
    const order = ordersFromStorage().find((item) => item.orderNo === orderMatch[1])
    return order ? ok(order) : fail(99001, '订单不存在或无权访问')
  }

  const cancelMatch = path.match(/^\/orders\/([^/]+)\/cancel$/)
  if (cancelMatch && method === 'post') {
    const list = ordersFromStorage()
    const order = list.find((item) => item.orderNo === cancelMatch[1])
    if (!order) return fail(99001, '订单不存在或无权访问')
    if (!['PENDING_PAYMENT', 'PENDING_SHIPMENT'].includes(order.status)) return fail(30002, '当前订单状态不允许取消')
    order.status = 'CANCELLED'
    write(ORDER_KEY, list)
    return ok(order)
  }

  const confirmMatch = path.match(/^\/orders\/([^/]+)\/confirm$/)
  if (confirmMatch && method === 'post') {
    const list = ordersFromStorage()
    const order = list.find((item) => item.orderNo === confirmMatch[1])
    if (!order) return fail(99001, '订单不存在或无权访问')
    if (order.status !== 'PENDING_RECEIPT') return fail(30002, '当前订单状态不允许确认收货')
    order.status = 'COMPLETED'
    write(ORDER_KEY, list)
    return ok(order)
  }

  if (path === '/payments' && method === 'post') {
    const list = ordersFromStorage()
    const order = list.find((item) => item.orderNo === body.orderNo)
    if (!order) return fail(99001, '订单不存在或无权访问')
    if (order.status !== 'PENDING_PAYMENT') return fail(30002, '当前订单状态不允许支付')
    order.status = 'PENDING_SHIPMENT'
    write(ORDER_KEY, list)
    return ok({ payNo: `PAY${Date.now()}`, orderNo: order.orderNo, amount: order.payAmount, status: 'PAID', method: body.method || 'MOCK' })
  }

  const paymentMatch = path.match(/^\/payments\/([^/]+)$/)
  if (paymentMatch && method === 'get') {
    return ok({ payNo: paymentMatch[1], status: 'PAID' })
  }

  if (path === '/after-sales' && method === 'get') {
    return ok({ list: read(AFTER_SALE_KEY, []), total: read(AFTER_SALE_KEY, []).length, pageNum: 1, pageSize: 20 })
  }

  if (path === '/after-sales' && method === 'post') {
    const list = read(AFTER_SALE_KEY, [])
    const sale = { ...body, saleNo: `AS${Date.now()}`, status: 'PENDING', createdAt: new Date().toLocaleString('zh-CN', { hour12: false }) }
    write(AFTER_SALE_KEY, [sale, ...list])
    return ok(sale)
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
