import { defineStore } from 'pinia'
import { ref } from 'vue'
import { login as loginApi, getProfile, logout as logoutApi } from '@/api/user'

export const useUserStore = defineStore('user', () => {
  const token = ref(localStorage.getItem('token') || '')
  const userInfo = ref(JSON.parse(localStorage.getItem('userInfo') || 'null'))

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

  return { token, userInfo, isLoggedIn, login, fetchProfile, logout }
})
