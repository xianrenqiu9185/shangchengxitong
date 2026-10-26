import request from '@/utils/request'

export const previewOrder = (data) => request.post('/orders/preview', data)
export const createOrder = (data) => request.post('/orders', data, { idempotent: true })
export const getOrders = (params) => request.get('/orders', { params })
export const getOrder = (orderNo) => request.get(`/orders/${orderNo}`)
export const cancelOrder = (orderNo) => request.post(`/orders/${orderNo}/cancel`, {}, { idempotent: true })
export const confirmOrder = (orderNo) => request.post(`/orders/${orderNo}/confirm`, {}, { idempotent: true })
export const payOrder = (data) => request.post('/payments', data, { idempotent: true })
export const getPayment = (payNo) => request.get(`/payments/${payNo}`)
