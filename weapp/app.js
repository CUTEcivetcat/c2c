const request = require('./utils/request.js')
const api = require('./utils/api.js')

App({
  globalData: {
    // 服务器地址：默认按编译环境自动选择；如需固定某个地址，直接改 serverUrl 覆盖即可
    serverUrl: '',
    token: '',
    userInfo: null
  },

  onLaunch() {
    // 环境区分：开发版/体验版走本地后端，正式版走测试服务器（按需修改两个地址）
    var env = ''
    try { env = wx.getAccountInfoSync().miniProgram.envVersion || '' } catch (e) {}
    var devServer = 'http://127.0.0.1:8080/api/v1'
    // 测试服务器；历史公网服务器已下线（真实公网地址不写入仓库）
    var prodServer = 'http://192.168.163.135:8080/api/v1'
    this.globalData.serverUrl = env === 'release' ? prodServer : devServer
    const token = wx.getStorageSync('token')
    const userInfo = wx.getStorageSync('userInfo')
    if (token) {
      this.globalData.token = token
      this.globalData.userInfo = userInfo ? JSON.parse(userInfo) : null
    }
  },

  // 保存登录态（微信 / 验证码 / 密码登录统一走这里）
  saveLogin: function (data) {
    this.globalData.token = data.token
    this.globalData.userInfo = data.userInfo
    wx.setStorageSync('token', data.token)
    wx.setStorageSync('userInfo', JSON.stringify(data.userInfo || null))
  },

  // 清空登录态
  clearLogin: function () {
    this.globalData.token = ''
    this.globalData.userInfo = null
    wx.removeStorageSync('token')
    wx.removeStorageSync('userInfo')
  },

  // 从后端拉取最新账户信息并同步到全局 + 本地缓存
  // 凡涉及账户信息变更的操作（绑定邮箱、修改资料、登录等）后都应调用，保证各页展示一致
  refreshUserInfo: function (cb) {
    var that = this
    if (!this.globalData.token) {
      if (cb) cb(null)
      return
    }
    api.getProfile().then(function (p) {
      if (p) {
        that.globalData.userInfo = p
        wx.setStorageSync('userInfo', JSON.stringify(p))
      }
      if (cb) cb(p)
    }).catch(function () {
      if (cb) cb(null)
    })
  },

  // 微信一键登录
  wechatLogin: function (callback) {
    var that = this
    wx.login({
      success: function (res) {
        request.login({
          code: res.code,
          nickname: '',
          avatarUrl: ''
        }).then(function (loginData) {
          that.saveLogin(loginData)
          if (callback) callback(null, loginData)
        }).catch(function (err) {
          if (callback) callback(err)
        })
      },
      fail: function (err) {
        if (callback) callback(err)
      }
    })
  },

  // 登录守卫：已登录执行回调，否则提示并跳"我的"页
  ensureLogin: function (cb) {
    if (this.globalData.token) {
      if (cb) cb()
      return true
    }
    wx.showToast({ title: '请先登录', icon: 'none' })
    setTimeout(function () {
      wx.switchTab({ url: '/pages/mine/mine' })
    }, 600)
    return false
  },

  // 拉取强制弹窗公告（登录后调用，带最低停留秒数）
  // 返回 Promise<公告对象|null>，由页面用自定义弹层展示
  checkForceAnnouncement: function () {
    var that = this
    if (!this.globalData.token) return Promise.resolve(null)
    return api.getAnnouncementForce().then(function (list) {
      if (!list || !list.length) return null
      var a = list[0]
      var lastDismiss = wx.getStorageSync('force_announcement_dismiss') || 0
      if (a.id && lastDismiss === a.id) return null
      return a
    }).catch(function () { return null })
  },

  // 标记某强制公告已关闭
  markForceAnnouncementDismissed: function (id) {
    wx.setStorageSync('force_announcement_dismiss', id || 0)
  },

  // 登录成功后全局检查强制公告：
  // 当前在"我的"页 → 由 mine 页自定义弹层展示（含秒数锁定）
  // 当前在其他页面 → 用系统弹窗摘要提示（简化实现，可关闭）
  maybeShowForceAnnouncement: function () {
    var that = this
    this.checkForceAnnouncement().then(function (a) {
      if (!a) return
      var pages = getCurrentPages()
      var cur = pages.length ? pages[pages.length - 1] : null
      if (cur && cur.route === 'pages/mine/mine') return
      var content = a.content || ''
      if (content.length > 160) content = content.substring(0, 160) + '…'
      wx.showModal({
        title: a.title || '平台公告',
        content: content,
        showCancel: false,
        confirmText: '我知道了',
        confirmColor: '#ff6b35',
        success: function () {
          that.markForceAnnouncementDismissed(a.id)
        }
      })
    })
  }
})
