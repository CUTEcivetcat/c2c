const api = require('../../../utils/api.js')
const util = require('../../../utils/util.js')

const STATUS_TEXT = { 0: '待支付', 1: '待发货', 2: '待收货', 3: '已收货', 4: '已完成', 5: '已取消' }

Page({
  data: {
    tab: 'buy',
    statusTabs: [
      { key: '', label: '全部' },
      { key: '0', label: '待支付' },
      { key: '1', label: '待发货' },
      { key: '2', label: '待收货' },
      { key: '4', label: '已完成' },
      { key: '5', label: '已取消' }
    ],
    status: '',
    orders: [],
    page: 1,
    size: 20,
    loading: false,
    hasMore: true
  },

  onLoad: function (opt) {
    if (opt && opt.tab) this.setData({ tab: opt.tab === 'sell' ? 'sell' : 'buy' })
  },

  onShow: function () {
    this.reload()
  },

  onPullDownRefresh: function () {
    this.reload(true)
  },

  onReachBottom: function () {
    if (this.data.hasMore && !this.data.loading) this.load()
  },

  switchTab: function (e) {
    if (e.currentTarget.dataset.t === this.data.tab) return
    this.setData({ tab: e.currentTarget.dataset.t })
    this.reload()
  },

  switchStatus: function (e) {
    var key = e.currentTarget.dataset.k
    if (key === this.data.status) return
    this.setData({ status: key })
    this.reload()
  },

  reload: function (stopPull) {
    this.setData({ page: 1, hasMore: true }, () => {
      this.load(true, stopPull)
    })
  },

  load: function (reset, stopPull) {
    if (this.data.loading) return
    var that = this
    var tab = this.data.tab
    var status = this.data.status === '' ? undefined : Number(this.data.status)
    this.setData({ loading: true })
    var params = { page: this.data.page, size: this.data.size }
    if (status !== undefined) params.status = status
    var req = tab === 'sell' ? api.getSellOrders(params) : api.getBuyOrders(params)
    req.then(function (r) {
      var list = r.records || []
      var merged = reset ? list : that.data.orders.concat(list)
      that.setData({
        orders: merged,
        page: that.data.page + 1,
        hasMore: merged.length < r.total,
        loading: false
      })
      if (stopPull) wx.stopPullDownRefresh()
    }).catch(function () {
      that.setData({ loading: false })
      if (stopPull) wx.stopPullDownRefresh()
    })
  },

  statusText: function (s) { return STATUS_TEXT[s] || '' },
  fmt: function (t) { return util.formatDateTime(t) },

  go: function (e) {
    wx.navigateTo({ url: '/pages/order/detail/detail?id=' + e.currentTarget.dataset.id })
  }
})
