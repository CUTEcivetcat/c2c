import axios from 'axios'
import { ElMessage } from 'element-plus'

const request = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_PATH || '/api/v1',
  timeout: 15000
})
request.interceptors.request.use(config => {
  const token = localStorage.getItem('admin_token')
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
}, error => Promise.reject(error))
// 跳转登录页（清除失效的 admin_token）
const goLogin = (msg) => {
  localStorage.removeItem('admin_token')
  window.location.hash = '#/login'
  ElMessage.error(msg || '登录已过期，请重新登录')
}

request.interceptors.response.use(response => {
  const { code, message, data } = response.data
  if (code === 200) return data
  // 登录接口失败（业务码 401）不跳转，由登录页提示
  if (code === 401 && !response.config.url.includes('/admin/login')) {
    goLogin(message)
    return Promise.reject(new Error(message))
  }
  ElMessage.error(message || '请求失败')
  return Promise.reject(new Error(message))
}, error => {
  const status = error.response?.status
  const message = error.response?.data?.message
  // 过滤器拦下的无效/过期 token 是 HTTP 401
  if (status === 401 && !error.config?.url?.includes('/admin/login')) {
    goLogin(message)
  } else {
    ElMessage.error(message || '网络异常')
  }
  return Promise.reject(error)
})

export default request

// Admin API — 全部走 admin 服务聚合
export const adminApi = {
  adminLogin: (data) => request.post('/admin/login', data),
  getSummary: () => request.get('/admin/dashboard/summary'),
  getTrends: () => request.get('/admin/dashboard/trends'),
  getUsers: (params) => request.get('/admin/users', { params }),
  toggleUserStatus: (id, status) => request.put(`/admin/users/${id}/status`, { status }),
  getProducts: (params) => request.get('/admin/products', { params }),
  toggleProductStatus: (id, status) => request.put(`/admin/products/${id}/status`, null, { params: { status } }),
  banProduct: (id, reason) => request.put(`/product/admin/${id}/ban`, { reason }),
  restoreProduct: (id) => request.put(`/product/admin/${id}/restore`),
  getOrders: (params) => request.get('/admin/orders', { params }),
  getOrderWallet: (orderId) => request.get(`/admin/orders/${orderId}/wallet`),
  // 权限管理
  getUserRoles: (params) => request.get('/admin/users/roles', { params }),
  setUserRole: (id, role) => request.put(`/admin/users/${id}/role`, { role }),
  // 审核管理（举报 + 整改申诉）
  getReports: (params) => request.get('/review/reports', { params }),
  getReportDetail: (id) => request.get(`/review/reports/${id}`),
  handleReport: (id, data) => request.post(`/review/reports/${id}/handle`, data),
  getAppeals: (params) => request.get('/review/appeals', { params }),
  getAppealDetail: (id) => request.get(`/review/appeals/${id}`),
  handleAppeal: (id, data) => request.post(`/review/appeals/${id}/handle`, data),
  // 公告管理
  getAnnouncements: (params) => request.get('/admin/announcement/list', { params }),
  createAnnouncement: (data) => request.post('/admin/announcement', data),
  updateAnnouncement: (id, data) => request.put(`/admin/announcement/${id}`, data),
  toggleAnnouncementStatus: (id, status) => request.put(`/admin/announcement/${id}/status`, { status }),
  deleteAnnouncement: (id) => request.delete(`/admin/announcement/${id}`),
  // 昵称审核
  getNicknameAudits: (params) => request.get('/review/nickname/list', { params }),
  handleNicknameAudit: (id, data) => request.post(`/review/nickname/${id}/handle`, data)
}
