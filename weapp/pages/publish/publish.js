const api = require('../../utils/api.js')
const app = getApp()
Page({
  data: {
    categories: [], catIndex: 0, catName: '',
    title: '', desc: '', price: '', originalPrice: '',
    condition: 0, conditionList: ['全新', '几乎全新', '轻微使用', '明显使用'],
    freight: 0, freightList: ['包邮', '买家承担', '自定'],
    freightAmount: '0', location: '', images: [], editingId: null, uploading: false
  },
  syncCatName: function () {
    var d = this.data
    var c = d.categories && d.categories[d.catIndex]
    this.setData({ catName: c ? c.name : '请选择' })
  },
  onLoad: function (o) {
    var that = this
    if (!app.globalData.token) { wx.switchTab({ url: '/pages/mine/mine' }) }
    api.getCategories().then(function (c) {
      that.setData({ categories: c || [] })
      that.syncCatName()
    })
    if (o && o.id) {
      this.editingId = o.id
      api.getProduct(o.id).then(function (p) {
        that.setData({
          title: p.title, desc: p.description, price: String(p.price),
          location: p.location || '',
          images: p.images ? p.images.map(function (i) { return i.url }) : []
        })
      })
    }
  },
  set1: function (e) { this.setData({ title: e.detail.value }) },
  set2: function (e) { this.setData({ desc: e.detail.value }) },
  set3: function (e) { this.setData({ price: e.detail.value }) },
  set4: function (e) { this.setData({ location: e.detail.value }) },
  set5: function (e) { this.setData({ originalPrice: e.detail.value }) },
  set6: function (e) { this.setData({ freightAmount: e.detail.value }) },
  onCat: function (e) {
    this.setData({ catIndex: e.detail.value })
    this.syncCatName()
  },
  onCond: function (e) { this.setData({ condition: e.detail.value }) },
  onFreight: function (e) { this.setData({ freight: e.detail.value }) },
  chooseImage: function () {
    var that = this
    wx.chooseImage({
      count: 9 - that.data.images.length, sizeType: ['compressed'],
      success: function (res) {
        that.setData({ uploading: true })
        var files = res.tempFilePaths
        var ups = []
        files.forEach(function (fp) { ups.push(api.upload(fp)) })
        Promise.all(ups).then(function (urls) {
          that.setData({ images: that.data.images.concat(urls), uploading: false })
        }).catch(function () {
          that.setData({ uploading: false })
          wx.showToast({ title: '上传失败', icon: 'none' })
        })
      }
    })
  },
  removeImg: function (e) {
    var i = e.currentTarget.dataset.i
    var a = this.data.images
    a.splice(i, 1)
    this.setData({ images: a })
  },
  submit: function () {
    var d = this.data
    if (!d.title) { wx.showToast({ title: '请输入标题', icon: 'none' }); return }
    if (!d.price || d.price <= 0) { wx.showToast({ title: '请输入价格', icon: 'none' }); return }
    var cat = d.categories[d.catIndex]
    if (!cat) { wx.showToast({ title: '请选择分类', icon: 'none' }); return }
    var body = {
      title: d.title, description: d.desc, price: d.price, originalPrice: d.originalPrice || null,
      categoryId: cat.id, condition: d.condition, freightType: Number(d.freight),
      freightAmount: Number(d.freightAmount) || 0, location: d.location, images: d.images
    }
    var that = this
    var req = this.editingId ? api.updateProduct(this.editingId, body) : api.publish(body)
    req.then(function () {
      wx.showToast({ title: '发布成功', icon: 'success' })
      setTimeout(function () { wx.switchTab({ url: '/pages/mine/mine' }) }, 600)
    }).catch(function () {})
  }
})