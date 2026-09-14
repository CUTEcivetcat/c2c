const api = require('../../utils/api.js')
const app = getApp()

// 后端成色枚举：1全新 2几乎全新 3轻微使用 4明显使用
const CONDITION_LIST = ['全新', '几乎全新', '轻微使用', '明显使用']
const FREIGHT_LIST = ['包邮', '买家承担', '自定']

Page({
  data: {
    categories: [], catOptions: [], catIndex: 0, catName: '请选择',
    title: '', desc: '', price: '', originalPrice: '',
    condition: 0, conditionList: CONDITION_LIST,
    freight: 0, freightList: FREIGHT_LIST,
    freightAmount: '', location: '',
    images: [], editingId: '', uploading: false,
    notices: []
  },

  onLoad: function (o) {
    var that = this
    if (!app.globalData.token) {
      wx.showToast({ title: '请先登录后发布', icon: 'none' })
      setTimeout(function () { wx.switchTab({ url: '/pages/mine/mine' }) }, 600)
      return
    }
    if (o && o.id) {
      this.editingId = o.id
      this.setData({ editingId: o.id })
    }
    api.getCategories().then(function (c) {
      that.setData({
        categories: c || [],
        catOptions: that.flattenCategories(c || [])
      })
      that.syncCatName()
      that.applyPendingFill()
    })
    // 发布页公告
    api.getAnnouncementPublish().then(function (list) {
      that.setData({ notices: list || [] })
    }).catch(function () {})
    if (o && o.id) {
      this.editingId = o.id
      // 优先用"我的发布"缓存的数据回填（下架商品详情接口会 404）
      var cached = wx.getStorageSync('editProductData')
      if (cached && cached.id == o.id) {
        that.fillForm(cached)
        wx.removeStorageSync('editProductData')
      }
      api.getProduct(o.id).then(function (p) {
        that.fillForm(p)
      }).catch(function () { /* 下架/已售商品走缓存回填 */ })
    }
  },

  // 统一表单回填（等待分类加载完成后再应用）
  fillForm: function (p) {
    this.pendingFill = p
    this.applyPendingFill()
  },

  applyPendingFill: function () {
    var p = this.pendingFill
    if (!p || !this.data.catOptions.length) return
    this.pendingFill = null
    var that = this
    this.setData({
      title: p.title,
      desc: p.description,
      price: String(p.price || ''),
      originalPrice: p.originalPrice ? String(p.originalPrice) : '',
      location: p.location || '',
      condition: p.condition ? p.condition - 1 : 0,
      freight: p.freightType || 0,
      freightAmount: p.freightAmount ? String(p.freightAmount) : '',
      images: (p.images || []).map(function (i) { return i.url || i })
    })
    // 回填分类：在平铺选项中定位 categoryId
    var opts = that.data.catOptions
    for (var i = 0; i < opts.length; i++) {
      if (opts[i].id === p.categoryId) { that.setData({ catIndex: i }); break }
    }
    that.syncCatName()
  },

  // 分类树 → 平铺选项（含二级，父级也保留）
  flattenCategories: function (cats) {
    var out = []
    if (!Array.isArray(cats)) return out
    cats.forEach(function (c) {
      out.push(c)
      if (c.children && c.children.length) {
        c.children.forEach(function (child) {
          var copy = { id: child.id, name: c.name + ' / ' + child.name }
          out.push(copy)
        })
      }
    })
    return out
  },

  syncCatName: function () {
    var opts = this.data.catOptions
    var c = opts && opts[this.data.catIndex]
    this.setData({ catName: c ? c.name : '请选择' })
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
    if (!d.price || Number(d.price) <= 0) { wx.showToast({ title: '请输入正确价格', icon: 'none' }); return }
    var cat = d.catOptions[d.catIndex]
    if (!cat) { wx.showToast({ title: '请选择分类', icon: 'none' }); return }
    var body = {
      title: d.title,
      description: d.desc,
      price: Number(d.price),
      originalPrice: d.originalPrice ? Number(d.originalPrice) : null,
      categoryId: cat.id,
      // 前端 0-3 → 后端 1-4
      condition: Number(d.condition) + 1,
      freightType: Number(d.freight),
      freightAmount: Number(d.freightAmount) || 0,
      location: d.location,
      images: d.images
    }
    var that = this
    var req = this.editingId ? api.updateProduct(this.editingId, body) : api.publish(body)
    req.then(function () {
      wx.showToast({ title: that.editingId ? '保存成功' : '发布成功', icon: 'success' })
      setTimeout(function () {
        if (that.editingId) wx.navigateBack()
        else wx.switchTab({ url: '/pages/mine/mine' })
      }, 600)
    }).catch(function () {})
  }
})
