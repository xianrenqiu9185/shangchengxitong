import request from '@/utils/request'

export const getMe = () => request.get('/users/me')
export const updateMe = (data) => request.put('/users/me', data)
export const getAddresses = () => request.get('/users/me/addresses')
export const createAddress = (data) => request.post('/users/me/addresses', data)
export const updateAddress = (id, data) => request.put(`/users/me/addresses/${id}`, data)
export const deleteAddress = (id) => request.delete(`/users/me/addresses/${id}`)
