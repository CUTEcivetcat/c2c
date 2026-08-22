const request = require('../../utils/request.js')

Page({
  data: {
    products: [],
    page: 1,
    size: 20,
    loading: false,
    hasMore: true
  },

  onLoad: function () {
    this.loadProducts()
  },

  onPullDownRefresh: function () {
    this.setData({ page: 1, hasMore: true })
    this.loadProducts(true)
  },

  onReachBottom: function () {
    if (this.data.hasMore && !this.data.loading) {
      this.loadProducts()
    }
  },

  loadProducts: function (refresh) {
    if (this.data.loading) return
    var that = this
    this.setData({ loading: true })
    request.get('/product/list', {
      page: this.data.page,
      size: this.data.size,
      sort: 'created_at'
    }).then(function (res) {
      var list = res.records || []
      var merged = refresh ? list : that.data.products.concat(list)
      that.setData({
        products: merged,
        page: that.data.page + 1,
        hasMore: merged.length < res.total,
        loading: false
      })
      if (refresh) wx.stopPullDownRefresh()
    }).catch(function () {
      that.setData({ loading: false })
      if (refresh) wx.stopPullDownRefresh()
    })
  },

  goProduct: function (e) {
    wx.navigateTo({ url: '/pages/product/product?id=' + e.currentTarget.dataset.id })
  },

  goMine: function () {
    wx.switchTab({ url: '/pages/mine/mine' })
  },

  goSearch: function () {
    wx.navigateTo({ url: '/pages/search/search' })
  }
})