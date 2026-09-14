const api = require('../../../utils/api.js')
const util = require('../../../utils/util.js')

const TYPE_TEXT = { 1: '公告', 2: '平台公约', 3: '通知' }

Page({
  data: {
    typeTabs: [
      { key: '', label: '全部' },
      { key: '1', label: '公告' },
      { key: '2', label: '平台公约' },
      { key: '3', label: '通知' }
    ],
    type: '',
    list: [],
    page: 1,
    size: 20,
    loading: false,
    hasMore: true,
    expandedId: null
  },

  onLoad: function () {
    this.load()
  },

  onReachBottom: function () {
    if (this.data.hasMore && !this.data.loading) this.load()
  },

  switchType: function (e) {
    var key = e.currentTarget.dataset.k
    if (key === this.data.type) return
    var that = this
    this.setData({ type: key, page: 1, hasMore: true }, function () {
      that.setData({ list: [] })
      that.load()
    })
  },

  load: function () {
    var that = this
    this.setData({ loading: true })
    var params = { page: this.data.page, size: this.data.size }
    if (this.data.type) params.type = Number(this.data.type)
    api.getAnnouncements(params).then(function (r) {
      var list = (r.records || []).map(function (x) {
        x._time = util.formatDateTime(x.createdAt)
        return x
      })
      var merged = that.data.list.concat(list)
      that.setData({
        list: merged,
        page: that.data.page + 1,
        hasMore: merged.length < r.total,
        loading: false
      })
    }).catch(function () { that.setData({ loading: false }) })
  },

  toggle: function (e) {
    var id = e.currentTarget.dataset.id
    this.setData({ expandedId: this.data.expandedId === id ? null : id })
  },

  typeText: function (t) { return TYPE_TEXT[t] || '' }
})
