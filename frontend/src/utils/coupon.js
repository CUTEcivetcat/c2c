// 优惠券展示口径工具（与后端 CouponTemplate 类型/用户券状态保持一致）

/** 满减券：满门槛减定额 */
export const COUPON_TYPE_FULL = 1
/** 折扣券：按折扣率打折 */
export const COUPON_TYPE_DISCOUNT = 2

/** 用户券状态：0 未使用 / 1 已使用 / 2 已过期 / 3 已作废 / 4 已锁定（待支付占用） */
export const COUPON_STATUS = { 0: '未使用', 1: '已使用', 2: '已过期', 3: '已作废', 4: '占用中' }

/** 面额文案：满减 ¥xx；折扣 x 折（0.9 → 9 折） */
export const couponFaceText = (c) => {
  if (Number(c.type) === COUPON_TYPE_DISCOUNT) {
    const rate = Number(c.discountAmount)
    const zhe = rate * 10
    return `${Number.isInteger(zhe) ? zhe : zhe.toFixed(1)}折`
  }
  return `¥${c.discountAmount}`
}

/** 门槛文案 */
export const couponThresholdText = (c) => {
  const min = Number(c.minAmount || 0)
  return min > 0 ? `满 ¥${min} 可用` : '无门槛'
}

/** 适用分类文案：未绑定分类视为全部分类通用 */
export const couponCategoryText = (c) => {
  if (!c.categoryId || Number(c.categoryId) === 0) return '全部分类'
  return c.categoryName || ('分类 #' + c.categoryId)
}

/** 有效期文案 */
export const couponValidText = (c) => {
  const fmt = (s) => s ? String(s).replace('T', ' ').slice(0, 16) : ''
  const start = fmt(c.validStart || c.receiveTime)
  const end = fmt(c.validEnd || c.expireTime)
  return end ? `${start ? start + ' ~ ' : ''}${end} 截止` : '长期有效'
}
