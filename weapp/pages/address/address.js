const api = require('../../utils/api.js')

Page({
  data: {
    list: [],
    loading: true,
    selectMode: false
  },
  onLoad: function (opt) {
    if (opt && opt.select == '1') this.setData({ selectMode: true })
  },
  onShow: function () { this.load() },
  load: function () {
    var t = this
    if (!getApp().globalData.token) { wx.switchTab({ url: '/pages/mine/mine' }); return }
    this.setData({ loading: true })
    api.getAddresses().then(function (list) {
      t.setData({ list: list || [], loading: false })
    }).catch(function () { t.setData({ loading: false }) })
  },
  choose: function (e) {
    if (!this.data.selectMode) return
    wx.setStorageSync('selectedAddressId', e.currentTarget.dataset.id)
    wx.navigateBack()
  },
  goAdd: function () {
    wx.navigateTo({ url: '/pages/address/edit' })
  },
  // 选择模式下切换到管理模式（编辑/删除），再点一次回到选择
  toggleMode: function () {
    this.setData({ selectMode: !this.data.selectMode })
    if (this.data.selectMode) wx.showToast({ title: '选择模式：点地址完成下单', icon: 'none' })
  },
  goEdit: function (e) {
    wx.navigateTo({ url: '/pages/address/edit?id=' + e.currentTarget.dataset.id })
  },
  setDefault: function (e) {
    var t = this
    api.setDefaultAddress(e.currentTarget.dataset.id).then(function () {
      wx.showToast({ title: '已设为默认', icon: 'none' })
      t.load()
    })
  },
  remove: function (e) {
    var id = e.currentTarget.dataset.id
    var t = this
    wx.showModal({ title: '删除地址', content: '确定删除该地址吗？', success: function (r) {
      if (r.confirm) api.deleteAddress(id).then(function () { t.load() })
    }})
  }
})