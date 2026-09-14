const api = require('../../../utils/api.js')
const util = require('../../../utils/util.js')
const app = getApp()

Page({
  data: {
    convId: null,
    otherId: null,
    otherName: '用户',
    productId: null,
    messages: [],
    input: '',
    sending: false,
    loading: true,
    hasMore: true,
    meId: '',
    scrollTop: 0
  },

  onLoad: function (opt) {
    this.setData({
      convId: Number(opt.convId),
      otherId: Number(opt.otherId),
      otherName: decodeURIComponent(opt.otherName || '用户'),
      productId: opt.productId ? Number(opt.productId) : null,
      meId: util.meId()
    })
    wx.setNavigationBarTitle({ title: this.data.otherName })
    this.firstId = null
    this.lastId = null
    this.loadInit()
    this.markRead()
    this.startPoll()
  },

  onShow: function () {
    this.startPoll()
    this.markRead()
  },

  onHide: function () {
    this.stopPoll()
  },

  onUnload: function () {
    this.stopPoll()
  },

  startPoll: function () {
    var that = this
    this.stopPoll()
    this.pollTimer = setInterval(function () { that.pollNew() }, 5000)
  },

  stopPoll: function () {
    if (this.pollTimer) { clearInterval(this.pollTimer); this.pollTimer = null }
  },

  loadInit: function () {
    var that = this
    this.setData({ loading: true })
    api.getMessages(this.data.convId, { size: 20 }).then(function (r) {
      var list = r.records || []
      that.firstId = list.length ? list[0].id : null
      that.lastId = list.length ? list[list.length - 1].id : null
      that.setData({ messages: list, loading: false, hasMore: list.length >= 20 })
    }).catch(function () {
      that.setData({ loading: false })
    })
  },

  // 上拉加载更早消息
  loadOlder: function () {
    if (!this.data.hasMore || this.data.loading) return
    var that = this
    this.setData({ loading: true })
    api.getMessages(this.data.convId, { before: this.firstId, size: 20 }).then(function (r) {
      var list = r.records || []
      that.firstId = list.length ? list[0].id : that.firstId
      that.setData({
        messages: list.concat(that.data.messages),
        loading: false,
        hasMore: list.length >= 20
      })
    }).catch(function () {
      that.setData({ loading: false })
    })
  },

  // 轮询新消息（增量）
  pollNew: function () {
    if (!this.lastId || !this.data.convId) return
    var that = this
    api.getMessages(this.data.convId, { after: this.lastId, size: 20 }).then(function (r) {
      var list = r.records || []
      if (!list.length) return
      that.lastId = list[list.length - 1].id
      that.setData({ messages: that.data.messages.concat(list) })
      that.markRead()
      that.scrollBottom()
    }).catch(function () {})
  },

  markRead: function () {
    api.markRead(this.data.convId).catch(function () {})
  },

  onInput: function (e) { this.setData({ input: e.detail.value }) },

  send: function () {
    var that = this
    var content = this.data.input.trim()
    if (!content) return
    if (content.length > 500) { wx.showToast({ title: '消息不能超过500字', icon: 'none' }); return }
    this.setData({ sending: true })
    api.sendMessage({ conversationId: this.data.convId, content: content }).then(function (msg) {
      that.lastId = msg.id
      if (!that.firstId) that.firstId = msg.id
      that.setData({
        messages: that.data.messages.concat([msg]),
        input: '',
        sending: false
      })
      that.scrollBottom()
    }).catch(function () {
      that.setData({ sending: false })
    })
  },

  scrollBottom: function () {
    this.setData({ scrollTop: (this.data.scrollTop || 0) + 99999 })
  },

  fmtTime: function (t) { return util.formatTime(t) },

  // 加载更多（页面底部上拉由 scroll-view 触发）
  onScrollToUpper: function () { this.loadOlder() }
})
