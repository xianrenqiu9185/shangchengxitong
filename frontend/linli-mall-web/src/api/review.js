import request from '@/utils/request'

export const createReview = (data) => request.post('/reviews', data)
