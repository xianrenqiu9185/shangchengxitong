import request from '@/utils/request'

export const getCategories = () => request.get('/categories')
export const getProducts = (params) => request.get('/products', { params })
export const getProduct = (id) => request.get(`/products/${id}`)
export const getProductReviews = (id) => request.get(`/products/${id}/reviews`)
