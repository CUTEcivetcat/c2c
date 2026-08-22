const api = require('../../utils/api.js')
Page({
  data: { categories: [], activeId: 0, products: [] },
  onLoad: function () { this.loadCategories() },
  loadCategories: function () {
    var that = this
    api.getCategories().then(function (cats) {
      that.setData({ categories: cats || [] })
      if (cats && cats.length) that.select(0)
    })
  },
  select: function (idx) {
    var c = this.data.categories[idx]
    this.setData({ activeId: idx })
    this.load(c.id)
  },
  load: function (cid) {
    var that = this
    var cat = this.data.categories[this.data.activeId]
    if (cat.level > 1 && cat.parentId) cid = cat.parentId
    api.getProducts({ categoryId: cid, page: 1, size: 50, sort: 'hot' }).then(function (res) {
      that.setData({ products: res.records || [] })
    })
  },
  goProduct: function (e) { wx.navigateTo({ url: '/pages/product/product?id=' + e.currentTarget.dataset.id }) }
})