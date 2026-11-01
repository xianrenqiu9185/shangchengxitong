import request from '@/utils/request'

export const login = (data) => request.post('/auth/login', data)
export const logout = () => request.post('/auth/logout', {}, { idempotent: true })
export const getMe = () => request.get('/users/me')

export const getDashboard = () => request.get('/admin/statistics/dashboard')

export const getProducts = (params) => request.get('/admin/products', { params })
export const createProduct = (data) => request.post('/admin/products', data)
export const updateProduct = (id, data) => request.put(`/admin/products/${id}`, data)
export const updateProductStatus = (id, status) => request.put(`/admin/products/${id}/status`, { status })
export const deleteProduct = (id) => request.delete(`/admin/products/${id}`)

export const getOrders = (params) => request.get('/admin/orders', { params })
export const getOrder = (orderNo) => request.get(`/admin/orders/${orderNo}`)
export const shipOrder = (orderNo) => request.post(`/admin/orders/${orderNo}/ship`, {}, { idempotent: true })
export const closeOrder = (orderNo) => request.post(`/admin/orders/${orderNo}/close`, {}, { idempotent: true })

export const getAfterSales = (params) => request.get('/admin/after-sales', { params })
export const auditAfterSale = (saleNo, data) => request.post(`/admin/after-sales/${saleNo}/audit`, data, { idempotent: true })

export const getResource = (endpoint, params) => request.get(endpoint, { params })
export const createResource = (endpoint, data) => request.post(endpoint, data)
export const updateResource = (endpoint, id, data) => request.put(`${endpoint}/${id}`, data)
export const deleteResource = (endpoint, id) => request.delete(`${endpoint}/${id}`)
