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
    this.setData({ tab: Number(e.currentTarget.dataset.t) })
  },
  onAccount: function (e) { this.setData({ account: e.detail.value }) },
  onPassword: function (e) { this.setData({ password: e.detail.value }) },
  onCode: function (e) { this.setData({ code: e.detail.value }) },

  saveSession: function (data) {
    getApp().saveLogin(data)
  },

  loginSuccess: function () {
    wx.showToast({ title: '登录成功', icon: 'success' })
    getApp().maybeShowForceAnnouncement()
    // 登录后引导绑定手机号（未绑定时）
    var u = getApp().globalData.userInfo
    if (u && !u.phone) {
      wx.showModal({
        title: '完善账号',
        content: '绑定手机号后更安全，还可用于登录与找回密码，是否现在绑定？',
        confirmText: '去绑定',
        cancelText: '暂不',
        confirmColor: '#ff6b35',
        success: function (res) {
          if (res.confirm) {
            wx.navigateTo({ url: '/pages/user/edit/edit?bind=phone' })
          } else {
            setTimeout(function () { wx.navigateBack({ fail: function () { wx.switchTab({ url: '/pages/mine/mine' }) } }) }, 300)
          }
        }
      })
    } else {
      setTimeout(function () { wx.navigateBack({ fail: function () { wx.switchTab({ url: '/pages/mine/mine' }) } }) }, 700)
    }
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
    api.sendSms(account).then(function (r) {
      // 开发环境验证码回显，直接填入输入框
      if (r && r.code) {
        t.setData({ code: r.code })
        wx.showToast({ title: '验证码已自动填入', icon: 'none' })
      } else {
        wx.showToast({ title: '已发送，请查收', icon: 'none' })
      }
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