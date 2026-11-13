import { defineStore } from 'pinia'
import { ref } from 'vue'
import { login as loginApi, getProfile, logout as logoutApi } from '@/api/user'

export const useUserStore = defineStore('user', () => {
  // S11（记档不改造）：token 存 localStorage，存在 XSS 可读风险；改 httpOnly Cookie 需后端登录/登出/刷新链路
  // 全量改为 Cookie 读写并补 CSRF 防护，收益/成本高，毕设阶段保留现状，由 401 清理 + token 版本化兜底缓解。
  const token = ref(localStorage.getItem('token') || '')
  const userInfo = ref(JSON.parse(localStorage.getItem('userInfo') || 'null'))

  // 登录弹窗（未登录操作时就地弹窗，不跳路由）：可见状态 + 登录成功后回调
  const loginDialogVisible = ref(false)
  let afterLoginCallback = null

  const openLoginDialog = (cb) => {
    afterLoginCallback = typeof cb === 'function' ? cb : null
    loginDialogVisible.value = true
  }
  const closeLoginDialog = () => {
    loginDialogVisible.value = false
    afterLoginCallback = null
  }
  const runAfterLogin = () => {
    const cb = afterLoginCallback
    afterLoginCallback = null
    if (cb) cb()
  }

  const isLoggedIn = () => !!token.value

  const login = async (data) => {
    const res = await loginApi(data)
    token.value = res.token
    userInfo.value = res.userInfo
    localStorage.setItem('token', res.token)
    localStorage.setItem('userInfo', JSON.stringify(res.userInfo))
    return res
  }

  const fetchProfile = async () => {
    try {
      const res = await getProfile()
      userInfo.value = res
      localStorage.setItem('userInfo', JSON.stringify(res))
    } catch (e) { /* */ }
  }

  const logout = async () => {
    try { await logoutApi() } catch (e) { /* */ }
    token.value = ''
    userInfo.value = null
    localStorage.removeItem('token')
    localStorage.removeItem('userInfo')
  }

  return { token, userInfo, isLoggedIn, login, fetchProfile, logout,
    loginDialogVisible, openLoginDialog, closeLoginDialog, runAfterLogin }
})
