import request from '@/utils/request'

export const getAfterSales = (params) => request.get('/after-sales', { params })
export const getAfterSale = (saleNo) => request.get(`/after-sales/${saleNo}`)
export const createAfterSale = (data) => request.post('/after-sales', data, { idempotent: true })
