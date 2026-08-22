const request = require('../../utils/request.js')
const app = getApp()

Page({
  data: { p: null, activeImage: '', buying: false, meId: '' },
  onLoad: function (opt) {
    var me = app.globalData.userInfo
    this.setData({ meId: me ? me.id : '' })
    if (opt.id) this.loadDetail(opt.id)
  },
  loadDetail: function (id) {
    var that = this
    request.get('/product/' + id).then(function (p) {
      that.setData({ p: p, activeImage: p.images && p.images[0] ? p.images[0].url : '' })
    })
  },
  changeImage: function (e) { this.setData({ activeImage: e.currentTarget.dataset.url }) },

  // 下单：先选收货地址
  buyNow: function () {
    var p = this.data.p
    if (!app.globalData.token) { wx.showToast({ title: '请先登录', icon: 'none' }); wx.switchTab({ url: '/pages/mine/mine' }); return }
    if (!this.data.meId || p.sellerId == this.data.meId) { wx.showToast({ title: '不能购买自己的商品', icon: 'none' }); return }
    wx.setStorageSync('pendingBuyProductId', p.id)
    wx.removeStorageSync('selectedAddressId')
    wx.navigateTo({ url: '/pages/address/address?select=1' })
  },

  // 从地址选择页返回
  onShow: function () {
    var pid = wx.getStorageSync('pendingBuyProductId')
    var addrId = wx.getStorageSync('selectedAddressId')
    if (pid && addrId) {
      wx.removeStorageSync('pendingBuyProductId')
      wx.removeStorageSync('selectedAddressId')
      this.confirmOrder(pid, addrId)
    }
  },

  confirmOrder: function (pid, addrId) {
    var p = this.data.p
    var that = this
    wx.showModal({ title: '下单', content: '余额支付 ¥' + p.price + '？', success: function (r) {
      if (!r.confirm) return
      that.setData({ buying: true })
      request.post('/order', { productId: pid, addressId: addrId }).then(function (order) {
        return request.post('/order/' + order.id + '/pay')
      }).then(function () {
        that.setData({ buying: false })
        wx.showToast({ title: '支付成功', icon: 'success' })
      }).catch(function () { that.setData({ buying: false }) })
    }})
  }
})