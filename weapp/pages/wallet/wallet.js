const api = require('../../utils/api.js')
const util = require('../../utils/util.js')
const app = getApp()

Page({
  data: {
    balance: '0.00',
    logs: [],
    showRecharge: false,
    amount: 100,
    loading: true
  },

  onShow: function () {
    var that = this
    if (!app.globalData.token) {
      wx.showToast({ title: '请先登录', icon: 'none' })
      setTimeout(function () { wx.switchTab({ url: '/pages/mine/mine' }) }, 500)
      return
    }
    this.load()
  },

  onPullDownRefresh: function () { this.load(true) },

  load: function (stopPull) {
    var t = this
    this.setData({ loading: true })
    api.walletProfile().then(function (r) {
      var logs = (r.logs || []).map(function (l) {
        l._time = util.formatDateTime(l.createdAt)
        l._type = { recharge: '充值', pay: '支付', refund: '退款', receive: '收款' }[l.type] || l.type || ''
        return l
      })
      t.setData({ balance: r.balance, logs: logs, loading: false })
      if (stopPull) wx.stopPullDownRefresh()
    }).catch(function () {
      t.setData({ loading: false })
      if (stopPull) wx.stopPullDownRefresh()
    })
  },

  show: function () { this.setData({ showRecharge: true }) },
  hide: function () { this.setData({ showRecharge: false }) },
  noop: function () {},
  onAmount: function (e) { this.setData({ amount: e.detail.value }) },
  recharge: function () {
    var t = this
    var amount = Number(this.data.amount)
    if (!amount || amount <= 0) { wx.showToast({ title: '请输入正确金额', icon: 'none' }); return }
    if (amount > 99999) { wx.showToast({ title: '单次上限99999元', icon: 'none' }); return }
    api.walletRecharge(amount).then(function () {
      t.setData({ showRecharge: false })
      t.load()
      wx.showToast({ title: '充值成功', icon: 'success' })
    }).catch(function () {})
  },

  type: function (t) {
    return { recharge: '充值', pay: '支付', refund: '退款', receive: '收款' }[t] || t || ''
  }
})
