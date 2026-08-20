import request from './request'

export const createOrder = (data) => request.post('/order', data)
export const getOrderDetail = (id) => request.get(`/order/${id}`)
export const getBuyerOrders = (params) => request.get('/order/list', { params })
export const getSellerOrders = (params) => request.get('/order/sell/list', { params })
export const payOrder = (id) => request.put(`/order/${id}/pay`)
export const shipOrder = (id, data) => request.put(`/order/${id}/ship`, data)
export const receiveOrder = (id) => request.put(`/order/${id}/receive`)
export const cancelOrder = (id, reason) => request.put(`/order/${id}/cancel`, { reason })
