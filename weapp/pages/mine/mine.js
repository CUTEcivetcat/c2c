const app = getApp()
const api = require('../../utils/api.js')

Page({
  data: {
    userInfo: null,
    loginSource: '',
    unread: 0,
    // 统计
    stats: { products: 0, favorites: 0, orders: 0 },
    // 强制公告
    forceNotice: null,
    forceRemain: 0,
    forceTimer: null
  },

  onShow: function () {
    var u = app.globalData.userInfo
    this.setData({ userInfo: u, loginSource: u ? (u.loginSource || '') : '' })
    if (u && app.globalData.token) {
      this.refreshProfile()
      this.loadUnread()
      this.loadStats()
      this.checkForce()
    } else {
      this.setData({ unread: 0, stats: { products: 0, favorites: 0, orders: 0 } })
      wx.removeTabBarBadge({ index: 2, fail: function () {} })
    }
  },

  onHide: function () {
    this.clearForceTimer()
  },
  onUnload: function () {
    this.clearForceTimer()
  },

  refreshProfile: function () {
    var t = this
    app.refreshUserInfo(function (p) {
      if (p) t.setData({ userInfo: p, loginSource: p.loginSource || '' })
    })
  },

  loadUnread: function () {
    var t = this
    api.unreadCount().then(function (r) {
      var n = (r && r.unreadTotal) || 0
      t.setData({ unread: n })
      // tabBar 角标同步
      if (n > 0) wx.setTabBarBadge({ index: 2, text: n > 99 ? '99+' : String(n) })
      else wx.removeTabBarBadge({ index: 2, fail: function () {} })
    }).catch(function () {})
  },

  // 统计：发布 / 收藏 / 订单（买+卖）
  loadStats: function () {
    var t = this
    Promise.all([
      api.getMyPublished({ page: 1, size: 1 }),
      api.getFavorites({ page: 1, size: 1 }),
      api.getBuyOrders({ page: 1, size: 1 }),
      api.getSellOrders({ page: 1, size: 1 })
    ]).then(function (rs) {
      t.setData({
        stats: {
          products: rs[0].total || 0,
          favorites: rs[1].total || 0,
          orders: (rs[2].total || 0) + (rs[3].total || 0)
        }
      })
    }).catch(function () {})
  },

  // ==================== 强制弹窗公告 ====================
  checkForce: function () {
    var t = this
    app.checkForceAnnouncement().then(function (a) {
      if (!a) return
      var min = (a.minSeconds || 0)
      t.setData({ forceNotice: a, forceRemain: min })
      if (min > 0) {
        t.forceTimer = setInterval(function () {
          var r = t.data.forceRemain - 1
          t.setData({ forceRemain: r })
          if (r <= 0) { t.clearForceTimer() }
        }, 1000)
      }
    })
  },
  clearForceTimer: function () {
    if (this.forceTimer) { clearInterval(this.forceTimer); this.forceTimer = null }
  },
  dismissForce: function () {
    if (this.data.forceRemain > 0) { wx.showToast({ title: '请阅读完公告', icon: 'none' }); return }
    app.markForceAnnouncementDismissed(this.data.forceNotice.id)
    this.clearForceTimer()
    this.setData({ forceNotice: null })
  },

  // ==================== 登录 ====================
  wechatLogin: function () {
    wx.showLoading({ title: '登录中' })
    var t = this
    app.wechatLogin(function (e) {
      wx.hideLoading()
      if (!e && app.globalData.userInfo) {
        t.setData({ userInfo: app.globalData.userInfo, loginSource: app.globalData.userInfo.loginSource || '' })
        wx.showToast({ title: '登录成功', icon: 'success' })
        t.refreshProfile()
        t.loadUnread()
        t.checkForce()
        app.maybeShowForceAnnouncement()
        // 登录后引导绑定手机号（未绑定时）
        var u = app.globalData.userInfo
        if (u && !u.phone) {
          setTimeout(function () {
            wx.showModal({
              title: '完善账号',
              content: '绑定手机号后更安全，还可用于登录与找回密码，是否现在绑定？',
              confirmText: '去绑定',
              cancelText: '暂不',
              confirmColor: '#ff6b35',
              success: function (res) {
                if (res.confirm) wx.navigateTo({ url: '/pages/user/edit/edit?bind=phone' })
              }
            })
          }, 600)
        }
      }
    })
  },

  nav: function (e) { wx.navigateTo({ url: e.currentTarget.dataset.url }) },
  noop: function () {},
  goAuthLogin: function () { wx.navigateTo({ url: '/pages/auth/login' }) },

  logout: function () {
    app.clearLogin()
    this.setData({ userInfo: null, loginSource: '', unread: 0 })
    wx.removeTabBarBadge({ index: 2, fail: function () {} })
  }
})
