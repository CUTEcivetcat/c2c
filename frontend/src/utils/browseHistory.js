// 浏览历史工具（localStorage 存储，最多 50 条，去重）
const KEY = 'c2c_browse_history'

export const recordBrowse = (productId) => {
  if (!productId) return
  let list = []
  try { list = JSON.parse(localStorage.getItem(KEY) || '[]') } catch (e) { list = [] }
  list = list.filter(id => String(id) !== String(productId))
  list.unshift(Number(productId))
  if (list.length > 50) list = list.slice(0, 50)
  localStorage.setItem(KEY, JSON.stringify(list))
}

export const getBrowseHistory = () => {
  try { return JSON.parse(localStorage.getItem(KEY) || '[]') } catch (e) { return [] }
}

export const clearBrowseHistory = () => localStorage.removeItem(KEY)