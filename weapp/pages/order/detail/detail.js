const api = require('../../../utils/api.js')
const util = require('../../../utils/util.js')
const app = getApp()

const STATUS_TEXT = { 0: '待支付', 1: '待发货', 2: '待收货', 3: '已收货', 4: '已完成', 5: '已取消' }

Page({
  data: {
    o: null,
    address: null,
    meId: '',
    isBuyer: false,
    isSeller: false,
    otherId: '',
    otherName: '',
    // 评价状态：我是否已评价
    myRated: false,
    myRateRole: 0, // 1 买家评卖家 / 2 卖家评买家
    statuses: STATUS_TEXT
  },

  onLoad: function (opt) {
    this.setData({ meId: util.meId() })
    this.id = opt.id
    this.load()
  },

  onShow: function () {
    // 从评价页返回后刷新评价状态
    if (this.data.o && this.data.o.status === 4) this.loadRatings()
  },

  loadRatings: function () {
    var that = this
    var o = this.data.o
    api.getOrderRatings(o.id).then(function (r) {
      var buyerRated = !!(r && r.buyerRating)
      var sellerRated = !!(r && r.sellerRating)
      if (o.buyerId == that.data.meId) that.setData({ myRated: buyerRated, myRateRole: 1 })
      if (o.sellerId == that.data.meId) that.setData({ myRated: sellerRated, myRateRole: 2 })
    }).catch(function () {})
  },

  load: function () {
    var that = this
    api.getOrder(this.id).then(function (o) {
      var meId = util.meId()
      var isBuyer = o.buyerId == meId
      var isSeller = o.sellerId == meId
      var otherId = isBuyer ? o.sellerId : o.buyerId
      var address = null
      try { address = JSON.parse(o.addressSnapshot || '{}') } catch (e) { address = {} }
      that.setData({
        o: o,
        address: address,
        isBuyer: isBuyer,
        isSeller: isSeller,
        otherId: otherId
      })
      // 对方昵称
      api.getUserPublicInfo(otherId).then(function (u) {
        that.setData({ otherName: (u && u.nickname) || '用户' + otherId })
      }).catch(function () {})
      // 评价状态（已完成订单）
      if (o.status === 4) that.loadRatings()
    }).catch(function () {})
  },

  fmt: function (t) { return util.formatDateTime(t) },

  // ==================== 操作 ====================
  pay: function () {
    var that = this
    wx.showModal({
      title: '余额支付',
      content: '确认使用余额支付 ¥' + (this.data.o ? this.data.o.totalAmount : 0) + ' ？',
      success: function (r) {
        if (!r.confirm) return
        api.payOrder(that.id).then(function () {
          wx.showToast({ title: '支付成功', icon: 'success' })
          that.load()
        }).catch(function () {})
      }
    })
  },

  ship: function () {
    var that = this
    wx.showModal({
      title: '发货',
      editable: true,
      placeholderText: '物流单号(可空)',
      success: function (r) {
        if (!r.confirm) return
        api.shipOrder(that.id, { shipCompany: '快递', shipNo: (r.content || '').trim() }).then(function () {
          wx.showToast({ title: '已发货', icon: 'success' })
          that.load()
        }).catch(function () {})
      }
    })
  },

  receive: function () {
    var that = this
    wx.showModal({
      title: '确认收货',
      content: '确认已收到商品？',
      success: function (r) {
        if (!r.confirm) return
        api.receiveOrder(that.id).then(function () {
          wx.showToast({ title: '已收货', icon: 'success' })
          that.load()
        }).catch(function () {})
      }
    })
  },

  cancel: function () {
    var that = this
    wx.showModal({
      title: '取消订单',
      content: '确定取消该订单？',
      success: function (r) {
        if (!r.confirm) return
        api.cancelOrder(that.id, '用户取消').then(function () {
          wx.showToast({ title: '已取消', icon: 'success' })
          that.load()
        }).catch(function () {})
      }
    })
  },

  goRate: function () {
    var o = this.data.o
    if (!o) return
    wx.navigateTo({
      url: '/pages/order/rating/rating?orderId=' + o.id +
           '&role=' + this.data.myRateRole +
           '&ratedUserId=' + (this.data.myRateRole === 1 ? o.sellerId : o.buyerId) +
           '&orderNo=' + encodeURIComponent(o.orderNo) +
           '&title=' + encodeURIComponent(o.productTitle)
    })
  },

  contactOther: function () {
    var that = this
    var o = this.data.o
    app.ensureLogin(function () {
      api.getOrCreateConversation({ targetUserId: that.data.otherId, productId: o.productId }).then(function (conv) {
        wx.navigateTo({
          url: '/pages/chat/detail/detail?convId=' + conv.id +
               '&otherId=' + that.data.otherId +
               '&otherName=' + encodeURIComponent(that.data.otherName || '用户') +
               '&productId=' + o.productId
        })
      }).catch(function () {})
    })
  },

  goProduct: function () {
    var o = this.data.o
    if (o && o.productId) wx.navigateTo({ url: '/pages/product/product?id=' + o.productId })
  },

  // 复制订单号（售后报单号用）
  copyOrderNo: function () {
    var o = this.data.o
    if (!o || !o.orderNo) return
    wx.setClipboardData({
      data: o.orderNo,
      success: function () { wx.showToast({ title: '订单号已复制', icon: 'none' }) }
    })
  }
})
