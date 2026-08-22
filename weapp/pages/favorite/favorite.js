const api = require('../../utils/api.js')
Page({
  data: { favs: [], loading: true },
  onShow: function () { this.load() },
  load: function () {
    var that = this
    this.setData({ loading: true })
    api.getFavorites({ page: 1, size: 100 }).then(function (r) {
      var rows = r.records || []
      var ids = rows.map(function (x) { return x.productId }).join(',')
      if (!ids) { that.setData({ favs: [], loading: false }); return }
      api.getProductsByIds(ids).then(function (ps) {
        that.setData({ favs: ps || [], loading: false })
      }).catch(function () { that.setData({ loading: false }) })
    }).catch(function () { that.setData({ loading: false }) })
  },
  go: function (e) { wx.navigateTo({ url: '/pages/product/product?id=' + e.currentTarget.dataset.id }) }
})