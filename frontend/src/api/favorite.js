import request from './request'

export const addFavorite = (productId) => request.post(`/favorite/${productId}`)
export const removeFavorite = (productId) => request.delete(`/favorite/${productId}`)
export const getFavorites = (params) => request.get('/favorite/list', { params })
export const checkFavorite = (productId) => request.get(`/favorite/check/${productId}`)
