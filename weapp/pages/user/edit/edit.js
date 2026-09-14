const api = require('../../../utils/api.js')
const app = getApp()

const GENDERS = ['保密', '男', '女']
// 手机号/邮箱脱敏展示
function maskPhone(p) { return p ? p.replace(/^(\d{3})\d{4}(\d{4})$/, '$1****$2') : '' }
function maskEmail(e) { if (!e) return ''; var i = e.indexOf('@'); return i < 2 ? e : e[0] + '***' + e.substring(i) }

Page({
  data: {
    avatarUrl: '',
    nickname: '',
    gender: 0,
    bio: '',
    genderList: GENDERS,
    saving: false,
    uploading: false,
    // 绑定状态
    phone: '',
    phoneCode: '',
    phoneBound: false,
    phoneDisplay: '',
    phoneMode: false,
    phoneCountdown: 0,
    phoneSending: false,
    phoneBinding: false,
    phoneEchoCode: '',
    email: '',
    emailCode: '',
    emailBound: false,
    emailDisplay: '',
    emailMode: false,
    emailCountdown: 0,
    emailSending: false,
    emailBinding: false,
    emailEchoCode: ''
  },

  onLoad: function (options) {
    var u = app.globalData.userInfo
    if (u) {
      this.setData({
        avatarUrl: u.avatarUrl || '',
        nickname: u.nickname || '',
        gender: u.gender || 0,
        bio: u.bio || ''
      })
    }
    this.refresh()
    // 登录后引导绑定：?bind=phone 自动展开手机绑定
    if (options && options.bind === 'phone') this.openPhoneBind()
  },

  refresh: function () {
    var that = this
    app.refreshUserInfo(function (p) {
      if (!p) return
      that.setData({
        avatarUrl: p.avatarUrl || '',
        nickname: p.nickname || '',
        gender: p.gender || 0,
        bio: p.bio || '',
        phoneBound: !!(p.phone),
        phoneDisplay: maskPhone(p.phone) || '',
        emailBound: !!(p.email),
        emailDisplay: maskEmail(p.email) || ''
      })
    })
  },

  onNickname: function (e) { this.setData({ nickname: e.detail.value }) },
  onBio: function (e) { this.setData({ bio: e.detail.value }) },
  onGender: function (e) { this.setData({ gender: Number(e.detail.value) }) },
  onPhone: function (e) { this.setData({ phone: e.detail.value }) },
  onPhoneCode: function (e) { this.setData({ phoneCode: e.detail.value }) },
  onEmail: function (e) { this.setData({ email: e.detail.value }) },
  onEmailCode: function (e) { this.setData({ emailCode: e.detail.value }) },

  chooseAvatar: function () {
    var that = this
    wx.chooseImage({
      count: 1, sizeType: ['compressed'],
      success: function (res) {
        that.setData({ uploading: true })
        api.upload(res.tempFilePaths[0]).then(function (url) {
          that.setData({ avatarUrl: url, uploading: false })
          wx.showToast({ title: '头像已上传', icon: 'none' })
        }).catch(function () {
          that.setData({ uploading: false })
          wx.showToast({ title: '上传失败', icon: 'none' })
        })
      }
    })
  },

  save: function () {
    var that = this
    var nickname = this.data.nickname.trim()
    if (!nickname) { wx.showToast({ title: '昵称不能为空', icon: 'none' }); return }
    if (nickname.length > 20) { wx.showToast({ title: '昵称最多20字', icon: 'none' }); return }
    this.setData({ saving: true })
    api.updateProfile({
      nickname: nickname,
      avatarUrl: this.data.avatarUrl,
      gender: this.data.gender,
      bio: this.data.bio.trim()
    }).then(function () {
      wx.showToast({ title: '保存成功', icon: 'success' })
      // 账户信息变更 → 统一走 app.refreshUserInfo 同步全局 + 缓存
      app.refreshUserInfo(function () {
        that.setData({ saving: false })
        setTimeout(function () { wx.navigateBack() }, 700)
      })
    }).catch(function () {
      that.setData({ saving: false })
    })
  },

  // ==================== 手机号绑定 ====================
  openPhoneBind: function () { this.setData({ phoneMode: true }) },
  closePhoneBind: function () { this.setData({ phoneMode: false, phoneEchoCode: '' }) },
  sendPhoneCode: function () {
    var that = this, phone = this.data.phone.trim()
    if (!/^1\d{10}$/.test(phone)) { wx.showToast({ title: '请输入正确的手机号', icon: 'none' }); return }
    if (this.data.phoneSending) return
    this.setData({ phoneSending: true })
    api.sendSms(phone).then(function (r) {
      that.setData({
        phoneSending: false,
        phoneEchoCode: (r && r.code) || '',
        phoneCode: (r && r.code) || that.data.phoneCode
      })
      wx.showToast({ title: (r && r.code) ? '验证码已回显' : '验证码已发送', icon: 'none' })
      var n = 60
      that.setData({ phoneCountdown: n })
      that._phoneTimer = setInterval(function () {
        n--
        if (n <= 0) { clearInterval(that._phoneTimer); that.setData({ phoneCountdown: 0 }) }
        else that.setData({ phoneCountdown: n })
      }, 1000)
    }).catch(function () { that.setData({ phoneSending: false }) })
  },
  bindPhoneAction: function () {
    var that = this
    var phone = this.data.phone.trim(), code = this.data.phoneCode.trim()
    if (!/^1\d{10}$/.test(phone)) { wx.showToast({ title: '请输入正确的手机号', icon: 'none' }); return }
    if (!code) { wx.showToast({ title: '请输入验证码', icon: 'none' }); return }
    this.setData({ phoneBinding: true })
    api.bindPhone({ phone: phone, smsCode: code }).then(function () {
      // 账户信息变更 → 同步账户信息
      app.refreshUserInfo(function (p) {
        that.setData({
          phoneBinding: false,
          phoneMode: false,
          phoneEchoCode: '',
          phoneBound: !!(p && p.phone),
          phoneDisplay: maskPhone(p && p.phone) || ''
        })
        wx.showToast({ title: '手机号绑定成功', icon: 'success' })
      })
    }).catch(function () { that.setData({ phoneBinding: false }) })
  },

  // ==================== 邮箱绑定 ====================
  openEmailBind: function () { this.setData({ emailMode: true }) },
  closeEmailBind: function () { this.setData({ emailMode: false, emailEchoCode: '' }) },
  sendEmailCode: function () {
    var that = this, email = this.data.email.trim()
    if (!email || email.indexOf('@') < 1) { wx.showToast({ title: '请输入正确的邮箱', icon: 'none' }); return }
    if (this.data.emailSending) return
    this.setData({ emailSending: true })
    api.sendSms(email).then(function (r) {
      that.setData({
        emailSending: false,
        emailEchoCode: (r && r.code) || '',
        emailCode: (r && r.code) || that.data.emailCode
      })
      wx.showToast({ title: (r && r.code) ? '验证码已回显' : '验证码已发送', icon: 'none' })
      var n = 60
      that.setData({ emailCountdown: n })
      that._emailTimer = setInterval(function () {
        n--
        if (n <= 0) { clearInterval(that._emailTimer); that.setData({ emailCountdown: 0 }) }
        else that.setData({ emailCountdown: n })
      }, 1000)
    }).catch(function () { that.setData({ emailSending: false }) })
  },
  bindEmailAction: function () {
    var that = this
    var email = this.data.email.trim(), code = this.data.emailCode.trim()
    if (!email || email.indexOf('@') < 1) { wx.showToast({ title: '请输入正确的邮箱', icon: 'none' }); return }
    if (!code) { wx.showToast({ title: '请输入验证码', icon: 'none' }); return }
    this.setData({ emailBinding: true })
    api.bindEmail({ email: email, code: code }).then(function () {
      // 账户信息变更 → 同步账户信息
      app.refreshUserInfo(function (p) {
        that.setData({
          emailBinding: false,
          emailMode: false,
          emailEchoCode: '',
          emailBound: !!(p && p.email),
          emailDisplay: maskEmail(p && p.email) || ''
        })
        wx.showToast({ title: '邮箱绑定成功', icon: 'success' })
      })
    }).catch(function () { that.setData({ emailBinding: false }) })
  },

  onUnload: function () {
    if (this._phoneTimer) clearInterval(this._phoneTimer)
    if (this._emailTimer) clearInterval(this._emailTimer)
  }
})
