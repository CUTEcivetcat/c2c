const api = require('../../../utils/api.js')
const util = require('../../../utils/util.js')
const app = getApp()

Page({
  data: { conversations: [], loading: true },

  onShow: function () {
    var that = this
    if (!app.globalData.token) {
      wx.showToast({ title: '请先登录', icon: 'none' })
      setTimeout(function () { wx.switchTab({ url: '/pages/mine/mine' }) }, 500)
      return
    }
    this.load()
    // 进入消息列表后，tabBar 角标先清零（详情页已读回执）
    api.unreadCount().then(function (r) {
      var n = (r && r.unreadTotal) || 0
      if (n > 0) wx.setTabBarBadge({ index: 2, text: n > 99 ? '99+' : String(n) })
      else wx.removeTabBarBadge({ index: 2, fail: function () {} })
    }).catch(function () {})
  },

  onPullDownRefresh: function () { this.load(true) },

  load: function (stopPull) {
    var that = this
    var meId = util.meId()
    api.getConversations().then(function (list) {
      var convs = list || []
      // 并行获取对方信息与商品信息
      var jobs = convs.map(function (c) {
        var otherId = c.user1Id == meId ? c.user2Id : c.user1Id
        var unread = c.user1Id == meId ? c.user1Unread : c.user2Unread
        c._otherId = otherId
        c._unread = unread || 0
        c._time = util.formatTime(c.lastMessageTime)
        return Promise.all([
          api.getUserPublicInfo(otherId).then(function (u) {
            c._otherName = (u && u.nickname) || '用户' + otherId
            c._otherAvatar = (u && u.avatarUrl) || ''
          }).catch(function () {
            c._otherName = '用户' + otherId
            c._otherAvatar = ''
          })
        ])
      })
      Promise.all(jobs).then(function () {
        that.setData({ conversations: convs, loading: false })
        if (stopPull) wx.stopPullDownRefresh()
      }).catch(function () {
        that.setData({ conversations: convs, loading: false })
        if (stopPull) wx.stopPullDownRefresh()
      })
    }).catch(function () {
      that.setData({ loading: false })
      if (stopPull) wx.stopPullDownRefresh()
    })
  },

  goChat: function (e) {
    var c = e.currentTarget.dataset.c
    wx.navigateTo({
      url: '/pages/chat/detail/detail?convId=' + c.id +
           '&otherId=' + c._otherId +
           '&otherName=' + encodeURIComponent(c._otherName || '用户') +
           '&productId=' + (c.productId || '')
    })
  }
})
