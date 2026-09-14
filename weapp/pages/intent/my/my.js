const api = require('../../../utils/api.js')
const util = require('../../../utils/util.js')
const app = getApp()

const STATUS_TEXT = { 1: '待处理', 2: '已回复', 3: '已成交', 4: '已关闭' }

Page({
  data: { list: [], statuses: STATUS_TEXT, loading: true },

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
    var that = this
    this.setData({ loading: true })
    api.getMyIntents({ page: 1, size: 50 }).then(function (r) {
      var list = (r.records || []).map(function (x) {
        x._time = util.formatDateTime(x.createdAt)
        return x
      })
      that.setData({ list: list, loading: false })
      if (stopPull) wx.stopPullDownRefresh()
    }).catch(function () {
      that.setData({ loading: false })
      if (stopPull) wx.stopPullDownRefresh()
    })
  },

  goProduct: function (e) {
    wx.navigateTo({ url: '/pages/product/product?id=' + e.currentTarget.dataset.id })
  },

  close: function (e) {
    var that = this
    var id = e.currentTarget.dataset.id
    wx.showModal({
      title: '关闭意向',
      content: '确定关闭该意向？',
      success: function (r) {
        if (!r.confirm) return
        api.closeIntent(id).then(function () {
          wx.showToast({ title: '已关闭', icon: 'none' })
          that.load()
        }).catch(function () {})
      }
    })
  }
})
