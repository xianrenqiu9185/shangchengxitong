import request from '@/utils/request'

export const getCoupons = () => request.get('/coupons', { silent: true })
export const receiveCoupon = (id) => request.post(`/coupons/${id}/receive`, {}, { idempotent: true })
export const getMyCoupons = () => request.get('/users/me/coupons', { silent: true })
