import axios from 'axios'
import { ElMessage } from 'element-plus'

const request = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_PATH || '/api/v1',
  timeout: 15000
})

// 请求拦截器
request.interceptors.request.use(
  config => {
    const token = localStorage.getItem('token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  error => Promise.reject(error)
)

// 响应拦截器
request.interceptors.response.use(
  response => {
    const { code, message, data } = response.data
    if (code === 200) return data
    if (code === 401) {
      localStorage.removeItem('token')
      localStorage.removeItem('userInfo')
      window.location.href = '/login'
      return Promise.reject(new Error('请先登录'))
    }
    ElMessage.error(message || '请求失败')
    return Promise.reject(new Error(message))
  },
  error => {
    const status = error.response?.status
    const msg = error.response?.data?.message
    if (status === 401) {
      // 后端返回真实 HTTP 401（AuthTokenFilter），清理本地登录态并跳登录
      localStorage.removeItem('token')
      localStorage.removeItem('userInfo')
      ElMessage.error(msg || '请先登录')
      if (!window.location.pathname.startsWith('/login')) window.location.href = '/login'
    } else {
      ElMessage.error(msg || '网络异常，请稍后重试')
    }
    return Promise.reject(error)
  }
)

export default request
