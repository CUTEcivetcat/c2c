const api = require('../../utils/api.js')

Page({
  data: {
    id: null,
    receiverName: '',
    phone: '',
    province: '',
    city: '',
    district: '',
    detail: '',
    postalCode: '',
    saving: false
  },
  onLoad: function (opt) {
    if (opt && opt.id) {
      this.setData({ id: opt.id })
      var t = this
      api.getAddress(opt.id).then(function (a) {
        t.setData({
          receiverName: a.receiverName || '',
          phone: a.phone || '',
          province: a.province || '',
          city: a.city || '',
          district: a.district || '',
          detail: a.detail || '',
          postalCode: a.postalCode || ''
        })
      })
    }
  },
  onInput: function (e) {
    var k = e.currentTarget.dataset.k
    var o = {}
    o[k] = e.detail.value
    this.setData(o)
  },
  save: function () {
    var d = this.data
    if (!d.receiverName.trim()) { wx.showToast({ title: '请填写收货人', icon: 'none' }); return }
    if (!/^1\d{10}$/.test(d.phone.trim())) { wx.showToast({ title: '请填写正确手机号', icon: 'none' }); return }
    if (!d.detail.trim()) { wx.showToast({ title: '请填写详细地址', icon: 'none' }); return }
    var body = {
      receiverName: d.receiverName.trim(),
      phone: d.phone.trim(),
      province: d.province.trim(),
      city: d.city.trim(),
      district: d.district.trim(),
      detail: d.detail.trim(),
      postalCode: d.postalCode.trim()
    }
    var t = this
    var req = d.id ? api.updateAddress(d.id, body) : api.addAddress(body)
    this.setData({ loading: true })
    req.then(function () {
      wx.showToast({ title: '保存成功', icon: 'success' })
      setTimeout(function () { wx.navigateBack() }, 600)
    }).catch(function () { t.setData({ loading: false }) })
  }
})