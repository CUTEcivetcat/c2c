import request from './request'

export const submitRating = (data) => request.post('/rating', data)
export const getUserRatings = (userId, params) => request.get(`/rating/user/${userId}`, { params })
export const getOrderRatings = (orderId) => request.get(`/rating/order/${orderId}`)
