import request from './request'

// 发送验证码（自动识别手机号/邮箱）
export const sendSms = (account) => request.post('/user/sms/send', null, { params: { account } })

// 统一登录（loginType: 1密码 2验证码）
export const login = (data) => request.post('/user/login', data)

// 注册
export const register = (data) => request.post('/user/register', data)

// 找回密码
export const resetPassword = (data) => request.post('/user/reset-password', data)

// 绑定手机号
export const bindPhone = (data) => request.post('/user/bind-phone', data)

// 退出
export const logout = () => request.post('/user/logout')

// 个人信息
export const getProfile = () => request.get('/user/profile')
export const updateProfile = (data) => request.put('/user/profile', null, { params: data })
export const getUserPublicInfo = (userId) => request.get(`/user/profile/${userId}`)

// 地址
export const getAddresses = () => request.get('/user/address')
export const addAddress = (data) => request.post('/user/address', data)
export const updateAddress = (id, data) => request.put(`/user/address/${id}`, data)
export const deleteAddress = (id) => request.delete(`/user/address/${id}`)
export const setDefaultAddress = (id) => request.put(`/user/address/${id}/default`)
