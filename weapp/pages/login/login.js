const request = require('../../utils/request.js')
const app = getApp()

Page({
  data: { email: '', code: '', countdown: 0, codeBtnText: '获取验证码', bindLoading: false },
  onEmail: function (e) { this.setData({ email: e.detail.value }) },
  onCode: function (e) { this.setData({ code: e.detail.value }) },
  sendCode: function () {
    var that = this, email = this.data.email
    if (!email || email.indexOf('@') < 0) { this.setData({ codeBtnText: '获取验证码' }); wx.showToast({ title: '请输入正确邮箱', icon: 'none' }); return }
    var timer;
    request.post('/user/sms/send?account=' + encodeURIComponent(email), {}).then(function () {
      wx.showToast({ title: '验证码已发送', icon: 'success' })
      var n = 60
      that.setData({ countdown: n, codeBtnText: n + 's' })
      timer = setInterval(function () {
        n--
        if (n <= 0) { clearInterval(timer); that.setData({ countdown: 0, codeBtnText: '获取验证码' }) }
        else { that.setData({ countdown: n, codeBtnText: n + 's' }) }
      }, 1000)
    }).catch(function () { that.setData({ codeBtnText: '获取验证码' }) })
  },
  bindEmail: function () {
    var email = this.data.email, code = this.data.code
    if (!email || !code) { wx.showToast({ title: '请填写邮箱和验证码', icon: 'none' }); return }
    var that = this
    this.setData({ bindLoading: true })
    request.post('/user/bind-email', { email: email, code: code }).then(function () {
      wx.showToast({ title: '绑定成功', icon: 'success' })
      setTimeout(function () { wx.navigateBack() }, 800)
    }).catch(function () { that.setData({ bindLoading: false }) })
  }
})
