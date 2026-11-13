import request from './request'

// 可领取券列表（分页）
export const getAvailableCoupons = (params) => request.get('/coupon/available', { params })
// 领取券
export const claimCoupon = (couponId) => request.post(`/coupon/${couponId}/claim`)
// 我的券（status：0 未使用 / 1 已使用 / 2 已过期 / 3 已作废）
export const getMyCoupons = (status) => request.get('/coupon/my', { params: { status } })
// 下单前抵扣试算
export const calcCoupon = (data) => request.post('/coupon/calc', data)
