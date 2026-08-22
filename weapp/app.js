const request = require('./utils/request.js')

App({
  globalData: {
    // 服务器地址：生产通过 nginx 反代 /api/v1 到后端 8080（微信开发者工具需勾选"不校验合法域名"）
    serverUrl: 'http://81.71.118.128/api/v1',
    token: '',
    userInfo: null
  },

  onLaunch() {
    const token = wx.getStorageSync('token')
    const userInfo = wx.getStorageSync('userInfo')
    if (token) {
      this.globalData.token = token
      this.globalData.userInfo = userInfo ? JSON.parse(userInfo) : null
    }
  },

  wechatLogin: function (callback) {
    var that = this
    wx.login({
      success: function (res) {
        request.login({
          code: res.code,
          nickname: '',
          avatarUrl: ''
        }).then(function (loginData) {
          that.globalData.token = loginData.token
          that.globalData.userInfo = loginData.userInfo
          wx.setStorageSync('token', loginData.token)
          wx.setStorageSync('userInfo', JSON.stringify(loginData.userInfo))
          if (callback) callback(null, loginData)
        }).catch(function (err) {
          if (callback) callback(err)
        })
      },
      fail: function (err) {
        if (callback) callback(err)
      }
    })
  }
})