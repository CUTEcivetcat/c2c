const app = getApp()
const api = require('../../utils/api.js')
Page({
  data: { userInfo: null, loginSource: '' },
  onShow: function () {
    var u = app.globalData.userInfo
    this.setData({ userInfo: u, loginSource: u ? (u.loginSource || '') : '' })
    if (u && app.globalData.token) this.refreshProfile()
  },
  refreshProfile: function () {
    var t = this
    api.getProfile().then(function (p) {
      if (p) {
        app.globalData.userInfo = p
        wx.setStorageSync('userInfo', JSON.stringify(p))
        t.setData({ userInfo: p, loginSource: p.loginSource || '' })
      }
    }).catch(function () {})
  },
  wechatLogin: function () {
    wx.showLoading({ title: '登录中' })
    var t = this
    app.wechatLogin(function (e) {
      wx.hideLoading()
      if (!e && app.globalData.userInfo) {
        t.setData({ userInfo: app.globalData.userInfo, loginSource: app.globalData.userInfo.loginSource || '' })
        wx.showToast({ title: '登录成功', icon: 'success' })
        t.refreshProfile()
      }
    })
  },
  nav: function (e) { wx.navigateTo({ url: e.currentTarget.dataset.url }) },
  goAuthLogin: function () { wx.navigateTo({ url: '/pages/auth/login' }) },
  logout: function () {
    app.globalData.token = ''
    app.globalData.userInfo = null
    wx.removeStorageSync('token')
    wx.removeStorageSync('userInfo')
    this.setData({ userInfo: null, loginSource: '' })
  }
})