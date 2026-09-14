const api = require('../../utils/api.js')
const app = getApp()

const TYPES = [
  { key: 1, label: '违禁品' },
  { key: 2, label: '假冒伪劣' },
  { key: 3, label: '描述不符' },
  { key: 4, label: '欺诈' },
  { key: 5, label: '侵权' },
  { key: 6, label: '其他' }
]

Page({
  data: {
    productId: null,
    productTitle: '',
    types: TYPES,
    typeIndex: 0,
    reason: '',
    submitting: false
  },

  onLoad: function (opt) {
    this.setData({
      productId: Number(opt.id),
      productTitle: opt.title ? decodeURIComponent(opt.title) : ''
    })
  },

  onType: function (e) { this.setData({ typeIndex: Number(e.detail.value) }) },
  onReason: function (e) { this.setData({ reason: e.detail.value }) },

  submit: function () {
    var that = this
    var reason = this.data.reason.trim()
    if (!reason) { wx.showToast({ title: '请填写举报理由', icon: 'none' }); return }
    if (reason.length < 5) { wx.showToast({ title: '请至少填写5个字', icon: 'none' }); return }
    this.setData({ submitting: true })
    api.createReport({
      productId: this.data.productId,
      reportType: this.data.types[this.data.typeIndex].key,
      reason: reason
    }).then(function () {
      wx.showToast({ title: '举报已提交', icon: 'success' })
      setTimeout(function () { wx.navigateBack() }, 700)
    }).catch(function () { that.setData({ submitting: false }) })
  }
})
