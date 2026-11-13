import request from './request'

// 钱包概况（余额 + 最近流水）
export const getWalletProfile = () => request.get('/user/wallet/profile')

// 银行卡方式模拟充值（body: {amount, cardId?}）
export const rechargeByCard = (data) => request.post('/user/wallet/recharge/card', data)

// 生成充值二维码票据 → { ticket }
export const createRechargeTicket = () => request.get('/user/wallet/recharge/ticket')

// H5 凭票据充值（公开，不走登录态；body: {ticket, amount}）
export const rechargeByTicket = (data) => request.post('/user/wallet/recharge/confirm', data)

// 模拟提现（body: {amount, cardId?}）
export const withdraw = (data) => request.post('/user/wallet/withdraw', data)

// 演示银行卡
export const listCards = () => request.get('/user/wallet/bank-cards')
export const addCard = (data) => request.post('/user/wallet/bank-cards', data)
export const removeCard = (id) => request.delete(`/user/wallet/bank-cards/${id}`)
export const setDefaultCard = (id) => request.put(`/user/wallet/bank-cards/${id}/default`)

// 生效中活动弹窗
export const getActivePopups = () => request.get('/popup/active')
