import request from './request'

export const getCategories = () => request.get('/product/category')
export const getProductDetail = (id) => request.get(`/product/${id}`)
export const searchProducts = (params) => request.get('/product/list', { params })
export const publishProduct = (data) => request.post('/product', data)
export const updateProduct = (id, data) => request.put(`/product/${id}`, data)
export const offShelfProduct = (id) => request.delete(`/product/${id}`)
// 更新商品状态（目标状态放 X-Status 请求头，如重新上架传 1）
export const updateStatus = (id, status) => request.put(`/product/${id}/status`, null, { headers: { 'X-Status': status } })
export const getMyPublished = (params) => request.get('/product/my/published', { params })
export const getUserProducts = (userId, params) => request.get(`/product/user/${userId}`, { params })
export const getProductsByIds = (ids) => request.get('/product/ids', { params: { ids } })
export const uploadImage = (file) => {
  const formData = new FormData()
  formData.append('file', file)
  return request.post('/upload/image', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

// ==================== 评论 ====================
export const getComments = (productId, params) => request.get('/product/comment', { params: { productId, ...params } })
export const addComment = (data) => request.post('/product/comment', data)
export const deleteComment = (id) => request.delete(`/product/comment/${id}`)

// ==================== 购买意向（我想要/询价砍价） ====================
export const createIntent = (productId, data) => request.post(`/product/intent/${productId}`, data)
export const getMyIntents = (params) => request.get('/product/intent/my', { params })
export const getSellerIntents = (params) => request.get('/product/intent/seller', { params })
export const replyIntent = (id, reply) => request.put(`/product/intent/${id}/reply`, { reply })
export const dealIntent = (id) => request.put(`/product/intent/${id}/deal`)
export const closeIntent = (id) => request.put(`/product/intent/${id}/close`)
