// 通用工具：时间格式化 / 价格格式化 / 登录守卫 / 节流

function pad(n) {
  return n < 10 ? '0' + n : '' + n
}

// 2026-09-13T12:30:00 → 今天 12:30 / 昨天 12:30 / 09-10 12:30 / 2025-12-31
function formatTime(t) {
  if (!t) return ''
  var d = new Date(String(t).replace('T', ' ').replace(/-/g, '/'))
  if (isNaN(d.getTime())) return String(t)
  var now = new Date()
  var sameDay = d.getFullYear() === now.getFullYear() && d.getMonth() === now.getMonth() && d.getDate() === now.getDate()
  var yest = new Date(now.getTime() - 86400000)
  var isYest = d.getFullYear() === yest.getFullYear() && d.getMonth() === yest.getMonth() && d.getDate() === yest.getDate()
  var hm = pad(d.getHours()) + ':' + pad(d.getMinutes())
  if (sameDay) return '今天 ' + hm
  if (isYest) return '昨天 ' + hm
  if (d.getFullYear() === now.getFullYear()) return pad(d.getMonth() + 1) + '-' + pad(d.getDate()) + ' ' + hm
  return d.getFullYear() + '-' + pad(d.getMonth() + 1) + '-' + pad(d.getDate())
}

// 完整时间 yyyy-MM-dd HH:mm
function formatDateTime(t) {
  if (!t) return ''
  var d = new Date(String(t).replace('T', ' ').replace(/-/g, '/'))
  if (isNaN(d.getTime())) return String(t)
  return d.getFullYear() + '-' + pad(d.getMonth() + 1) + '-' + pad(d.getDate()) + ' ' + pad(d.getHours()) + ':' + pad(d.getMinutes())
}

// 金额保留两位（字符串展示）
function money(v) {
  var n = Number(v || 0)
  return n.toFixed(2)
}

// 登录守卫：已登录执行回调，否则提示并跳"我的"页
function ensureLogin(cb) {
  var app = getApp()
  if (app && app.globalData && app.globalData.token) {
    if (cb) cb()
    return true
  }
  wx.showToast({ title: '请先登录', icon: 'none' })
  setTimeout(function () {
    wx.switchTab({ url: '/pages/mine/mine' })
  }, 600)
  return false
}

// 当前用户 id（未登录返回空串）
function meId() {
  var app = getApp()
  return (app && app.globalData && app.globalData.userInfo && app.globalData.userInfo.id) || ''
}

// 简单节流
function throttle(fn, wait) {
  var last = 0
  return function () {
    var now = Date.now()
    if (now - last > wait) {
      last = now
      fn.apply(this, arguments)
    }
  }
}

module.exports = {
  formatTime: formatTime,
  formatDateTime: formatDateTime,
  money: money,
  ensureLogin: ensureLogin,
  meId: meId,
  throttle: throttle
}
