const api = require('../../utils/api.js')

// 商品状态：1在售 2已预订 3已售 4已下架 5违规下架
const STATUS_TEXT = { 1: '在售', 2: '已预订', 3: '已售', 4: '已下架', 5: '违规下架' }

Page({
  data: {
    list: [],
    statuses: STATUS_TEXT,
    loading: true
  },

  onShow: function () { this.load() },

  onPullDownRefresh: function () {
    this.load(true)
  },

  load: function (stopPull) {
    var that = this
    this.setData({ loading: true })
    api.getMyPublished({ page: 1, size: 50 }).then(function (r) {
      that.setData({ list: r.records || [], loading: false })
      if (stopPull) wx.stopPullDownRefresh()
    }).catch(function () {
      that.setData({ loading: false })
      if (stopPull) wx.stopPullDownRefresh()
    })
  },

  go: function (e) { wx.navigateTo({ url: '/pages/product/product?id=' + e.currentTarget.dataset.id }) },
  // 编辑：列表里已带完整商品数据，先缓存给发布页（下架商品详情接口 404，靠缓存回填）
  edit: function (e) {
    var id = e.currentTarget.dataset.id
    var item = null
    for (var i = 0; i < this.data.list.length; i++) {
      if (this.data.list[i].id == id) { item = this.data.list[i]; break }
    }
    if (item) {
      item._images = (item.images || []).map(function (im) { return im.url })
      wx.setStorageSync('editProductData', item)
    }
    wx.navigateTo({ url: '/pages/publish/publish?id=' + id })
  },

  // 下架：仅状态为在售(1)/已预订(2)/已售(3)可下架
  off: function (e) {
    var that = this
    var id = e.currentTarget.dataset.id
    wx.showModal({
      title: '下架商品',
      content: '下架后其他用户将无法看到该商品，确定下架？',
      success: function (r) {
        if (!r.confirm) return
        api.offShelf(id).then(function () {
          wx.showToast({ title: '已下架', icon: 'success' })
          that.load()
        }).catch(function () {})
      }
    })
  },

  // 重新上架：仅状态为已下架(4)可上架
  onShelf: function (e) {
    var that = this
    var id = e.currentTarget.dataset.id
    wx.showModal({
      title: '重新上架',
      content: '确定重新上架该商品？',
      success: function (r) {
        if (!r.confirm) return
        api.updateProductStatus(id, 1).then(function () {
          wx.showToast({ title: '已上架', icon: 'success' })
          that.load()
        }).catch(function () {})
      }
    })
  }
})
