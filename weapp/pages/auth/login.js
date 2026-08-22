const api = require('../../utils/api.js')
const app = getApp()

Page({
  data: {
    tab: 0,
    account: '',
    password: '',
    code: '',
    countdown: 0,
    codeBtnText: '获取验证码',
    loading: false
  },
  switchTab: function (e) {
    this.setData({ tab: e.currentTarget.dataset.t })
  },
  onAccount: function (e) { this.setData({ account: e.detail.value }) },
  onPassword: function (e) { this.setData({ password: e.detail.value }) },
  onCode: function (e) { this.setData({ code: e.detail.value }) },

  saveSession: function (data) {
    var app = getApp()
    app.globalData.token = data.token
    app.globalData.userInfo = data.userInfo
    wx.setStorageSync('token', data.token)
    wx.setStorageSync('userInfo', JSON.stringify(data.userInfo || null))
  },

  loginSuccess: function () {
    wx.showToast({ title: '登录成功', icon: 'success' })
    setTimeout(function () { wx.navigateBack({ fail: function () { wx.switchTab({ url: '/pages/mine/mine' }) } }) }, 700)
  },

  // ===== 微信一键登录 =====
  wechatLogin: function () {
    var t = this
    this.setData({ loading: true })
    wx.showLoading({ title: '登录中' })
    getApp().wechatLogin(function (err, data) {
      wx.hideLoading()
      t.setData({ loading: false })
      if (!err && data && data.token) {
        t.loginSuccess()
      } else {
        wx.showToast({ title: '微信登录失败', icon: 'none' })
      }
    })
  },
  // ===== 验证码登录 =====
  sendCode: function () {
    var account = this.data.account.trim()
    if (!account) { wx.showToast({ title: '请输入手机号或邮箱', icon: 'none' }); return }
    var t = this
    api.sendSms(account).then(function () {
      wx.showToast({ title: '已发送，请查收', icon: 'none' })
      var n = 60
      t.setData({ countdown: n, codeBtnText: n + 's' })
      var timer = setInterval(function () {
        n--
        if (n <= 0) { clearInterval(timer); t.setData({ countdown: 0, codeBtnText: '获取验证码' }) }
        else { t.setData({ countdown: n, codeBtnText: n + 's' }) }
      }, 1000)
    }).catch(function () {})
  },
  loginByCode: function () {
    var account = this.data.account.trim()
    var code = this.data.code.trim()
    if (!account) { wx.showToast({ title: '请输入手机号或邮箱', icon: 'none' }); return }
    if (!code) { wx.showToast({ title: '请输入验证码', icon: 'none' }); return }
    var t = this
    this.setData({ loading: true })
    api.loginByCode(account, code).then(function (data) {
      t.saveSession(data)
      t.loginSuccess()
    }).catch(function () {
      t.setData({ loading: false })
    })
  },
  loginByPassword: function () {
    var account = this.data.account.trim()
    var password = this.data.password
    if (!account) { wx.showToast({ title: '请输入手机号或邮箱', icon: 'none' }); return }
    if (!password) { wx.showToast({ title: '请输入密码', icon: 'none' }); return }
    var t = this
    this.setData({ loading: true })
    api.loginByPassword(account, password).then(function (data) {
      t.saveSession(data)
      t.loginSuccess()
    }).catch(function () {
      t.setData({ loading: false })
    })
  }
})