import request from './request'

export const getBanners = () => request.get('/banner/list')
export const getBannersAdmin = () => request.get('/admin/banner/list')
export const createBanner = (data) => request.post('/admin/banner', data)
export const updateBanner = (id, data) => request.put(`/admin/banner/${id}`, data)
export const deleteBanner = (id) => request.delete(`/admin/banner/${id}`)