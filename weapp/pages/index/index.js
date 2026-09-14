const api = require('../../utils/api.js')

// 本地分类图标映射（后端无 iconUrl 时兜底，风格统一）
const CAT_ICONS = {
  '书籍': '/images/category/book.jpg',
  '数码': '/images/category/digital.jpg',
  '手机': '/images/category/phone.jpg',
  '日用': '/images/category/daily.jpg',
  '教材': '/images/category/textbook.jpg',
  '英语': '/images/category/english.jpg',
  '笔记本电脑': '/images/category/laptop.jpg',
  '苹果手机': '/images/category/apple-phone.jpg',
  '宿舍用品': '/images/category/dorm.jpg'
}

Page({
  data: {
    banners: [],
    notices: [],
    categories: [],
    products: [],
    sort: 'created_at',
    sortOptions: [
      { key: 'created_at', label: '最新' },
      { key: 'hot', label: '最热' },
      { key: 'price_asc', label: '价格↑' },
      { key: 'price_desc', label: '价格↓' }
    ],
    page: 1,
    size: 20,
    loading: false,
    hasMore: true
  },

  catIcon: function (name) { return CAT_ICONS[name] || '' },

  onLoad: function () {
    this.loadBanners()
    this.loadNotices()
    this.loadCategories()
    this.loadProducts()
  },

  onPullDownRefresh: function () {
    var that = this
    this.loadBanners()
    this.loadNotices()
    this.setData({ page: 1, hasMore: true }, function () {
      that.loadProducts(true)
    })
  },

  onReachBottom: function () {
    if (this.data.hasMore && !this.data.loading) {
      this.loadProducts()
    }
  },

  loadBanners: function () {
    var that = this
    api.getBanners().then(function (list) {
      // 过滤无图 banner，避免渲染出灰底空白
      var arr = (list || []).filter(function (b) { return b && b.imageUrl })
      that.setData({ banners: arr })
    }).catch(function () {})
  },

  // banner 图片加载失败 → 隐藏该项（不显示灰底空白）
  onBannerError: function (e) {
    var idx = e.currentTarget.dataset.index
    var arr = this.data.banners.slice()
    if (arr[idx]) { arr.splice(idx, 1); this.setData({ banners: arr }) }
  },

  loadNotices: function () {
    var that = this
    api.getAnnouncementLatest(3).then(function (list) {
      that.setData({ notices: list || [] })
    }).catch(function () {})
  },

  loadCategories: function () {
    var that = this
    api.getCategories().then(function (cats) {
      var tops = (cats || []).filter(function (c) { return c.level === 1 })
      that.setData({ categories: tops.length ? tops : (cats || []) })
    }).catch(function () {})
  },

  onSort: function (e) {
    var sort = e.currentTarget.dataset.key
    if (sort === this.data.sort) return
    var that = this
    this.setData({ sort: sort, page: 1, hasMore: true }, function () {
      that.loadProducts(true)
    })
  },

  loadProducts: function (refresh) {
    if (this.data.loading) return
    var that = this
    this.setData({ loading: true })
    api.getProducts({
      page: this.data.page,
      size: this.data.size,
      sort: this.data.sort
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

  goSearch: function () {
    wx.navigateTo({ url: '/pages/search/search' })
  },

  goCategory: function (e) {
    var id = e.currentTarget.dataset.id || ''
    wx.navigateTo({ url: '/pages/category/category' + (id ? '?id=' + id : '') })
  },

  goNotices: function () {
    wx.navigateTo({ url: '/pages/announcement/list/list' })
  },

  goMine: function () {
    wx.switchTab({ url: '/pages/mine/mine' })
  }
})
