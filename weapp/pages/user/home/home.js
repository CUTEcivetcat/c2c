const api = require('../../../utils/api.js')
const app = getApp()

Page({
  data: {
    uid: null,
    user: null,
    products: [],
    meId: '',
    isSelf: false,
    loading: true
  },

  onLoad: function (opt) {
    var uid = Number(opt.uid)
    this.setData({ uid: uid, meId: (app.globalData.userInfo && app.globalData.userInfo.id) || '' })
    if (!uid) { wx.showToast({ title: '参数错误', icon: 'none' }); return }
    this.load()
  },

  load: function () {
    var that = this
    api.getUserPublicInfo(this.data.uid).then(function (u) {
      that.setData({ user: u, isSelf: u.id == that.data.meId })
    }).catch(function () {})
    api.getUserProducts(this.data.uid, { page: 1, size: 50 }).then(function (r) {
      that.setData({ products: r.records || [], loading: false })
    }).catch(function () { that.setData({ loading: false }) })
  },

  goProduct: function (e) {
    wx.navigateTo({ url: '/pages/product/product?id=' + e.currentTarget.dataset.id })
  },

  contact: function () {
    var that = this
    app.ensureLogin(function () {
      if (that.data.isSelf) { wx.showToast({ title: '这是你自己', icon: 'none' }); return }
      api.getOrCreateConversation({ targetUserId: that.data.uid, productId: null }).then(function (conv) {
        var name = (that.data.user && that.data.user.nickname) || '用户'
        wx.navigateTo({
          url: '/pages/chat/detail/detail?convId=' + conv.id +
               '&otherId=' + that.data.uid +
               '&otherName=' + encodeURIComponent(name) +
               '&productId='
        })
      }).catch(function () {})
    })
  },

  goMine: function () { wx.switchTab({ url: '/pages/mine/mine' }) }
})
