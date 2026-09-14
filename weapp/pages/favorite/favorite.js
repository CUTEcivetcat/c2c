const api = require('../../utils/api.js')
const app = getApp()

Page({
  data: { favs: [], loading: true },

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
    api.getFavorites({ page: 1, size: 100 }).then(function (r) {
      var rows = r.records || []
      var ids = rows.map(function (x) { return x.productId }).join(',')
      if (!ids) {
        that.setData({ favs: [], loading: false })
        if (stopPull) wx.stopPullDownRefresh()
        return
      }
      api.getProductsByIds(ids).then(function (ps) {
        // 关联收藏记录 id 用于取消收藏
        var map = {}
        rows.forEach(function (x) { map[x.productId] = x.id })
        var favs = (ps || []).map(function (p) {
          p._favId = map[p.id]
          return p
        })
        that.setData({ favs: favs, loading: false })
        if (stopPull) wx.stopPullDownRefresh()
      }).catch(function () {
        that.setData({ loading: false })
        if (stopPull) wx.stopPullDownRefresh()
      })
    }).catch(function () {
      that.setData({ loading: false })
      if (stopPull) wx.stopPullDownRefresh()
    })
  },

  go: function (e) { wx.navigateTo({ url: '/pages/product/product?id=' + e.currentTarget.dataset.id }) },

  // 取消收藏：先跳商品页或直接移除？直接移除更顺
  remove: function (e) {
    var that = this
    var id = e.currentTarget.dataset.id
    wx.showModal({
      title: '取消收藏',
      content: '确定取消收藏该商品？',
      success: function (r) {
        if (!r.confirm) return
        api.unFavorite(id).then(function () {
          wx.showToast({ title: '已取消', icon: 'none' })
          that.load()
        }).catch(function () {})
      }
    })
  }
})
