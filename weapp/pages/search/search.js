const api = require('../../utils/api.js')

const CONDITIONS = [
  { key: '', label: '全部成色' },
  { key: '1', label: '全新' },
  { key: '2', label: '几乎全新' },
  { key: '3', label: '轻微使用' },
  { key: '4', label: '明显使用' }
]

Page({
  data: {
    keyword: '',
    products: [],
    hot: ['教材', '手机', '电脑', '相机', '耳机', '自行车'],
    history: [],
    showFilter: false,
    conditions: CONDITIONS,
    condition: '',
    minPrice: '',
    maxPrice: '',
    sort: 'created_at',
    sortOptions: [
      { key: 'created_at', label: '最新' },
      { key: 'hot', label: '最热' },
      { key: 'price_asc', label: '价格↑' },
      { key: 'price_desc', label: '价格↓' }
    ],
    loading: false
  },

  onLoad: function () {
    this.setData({ history: wx.getStorageSync('search_history') || [] })
  },

  onInput: function (e) { this.setData({ keyword: e.detail.value }) },

  onSort: function (e) {
    this.setData({ sort: e.currentTarget.dataset.key })
    if (this.data.keyword.trim()) this.search(this.data.keyword)
  },

  doSearch: function () {
    var kw = this.data.keyword.trim()
    if (!kw) return
    var h = this.data.history
    if (h.indexOf(kw) < 0) {
      h.unshift(kw)
      if (h.length > 10) h = h.slice(0, 10)
      this.setData({ history: h })
      wx.setStorageSync('search_history', h)
    }
    this.search(kw)
  },

  quick: function (e) {
    this.setData({ keyword: e.currentTarget.dataset.w })
    this.search(e.currentTarget.dataset.w)
  },

  clearHistory: function () {
    wx.removeStorageSync('search_history')
    this.setData({ history: [] })
  },

  // ==================== 筛选 ====================
  toggleFilter: function () { this.setData({ showFilter: !this.data.showFilter }) },
  noop: function () {},
  selectCondition: function (e) { this.setData({ condition: e.currentTarget.dataset.k }) },
  onMinPrice: function (e) { this.setData({ minPrice: e.detail.value }) },
  onMaxPrice: function (e) { this.setData({ maxPrice: e.detail.value }) },
  applyFilter: function () {
    this.setData({ showFilter: false })
    var kw = this.data.keyword.trim()
    if (kw) this.search(kw)
  },
  resetFilter: function () {
    this.setData({ condition: '', minPrice: '', maxPrice: '' })
  },

  search: function (kw) {
    var that = this
    var params = {
      keyword: kw,
      page: 1,
      size: 50,
      sort: this.data.sort
    }
    if (this.data.condition) params.condition = Number(this.data.condition)
    if (this.data.minPrice) params.minPrice = Number(this.data.minPrice)
    if (this.data.maxPrice) params.maxPrice = Number(this.data.maxPrice)
    this.setData({ loading: true })
    api.getProducts(params).then(function (r) {
      that.setData({ products: r.records || [], loading: false })
    }).catch(function () { that.setData({ loading: false }) })
  },

  goProduct: function (e) {
    wx.navigateTo({ url: '/pages/product/product?id=' + e.currentTarget.dataset.id })
  }
})
