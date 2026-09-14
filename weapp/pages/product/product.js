const api = require('../../utils/api.js')
const util = require('../../utils/util.js')
const app = getApp()

Page({
  data: {
    p: null,
    activeImage: '',
    meId: '',
    isOwn: false,
    comments: [],
    commentPage: 1,
    commentLoading: false,
    hasMoreComment: true,
    commentText: '',
    commentSending: false,
    // 询价弹层
    intentSheet: false,
    intentMsg: '',
    intentPrice: '',
    sending: false,
    buying: false
  },

  onLoad: function (opt) {
    this.setData({ meId: util.meId() })
    if (opt.id) this.loadDetail(opt.id)
  },

  onShow: function () {
    // 从地址选择页返回 → 发起下单
    var pid = wx.getStorageSync('pendingBuyProductId')
    var addrId = wx.getStorageSync('selectedAddressId')
    if (pid && addrId) {
      wx.removeStorageSync('pendingBuyProductId')
      wx.removeStorageSync('selectedAddressId')
      this.confirmOrder(pid, addrId)
      return
    }
    // 从编辑/其他页返回 → 刷新最新数据
    if (this.id && this.data.p) this.loadDetail(this.id)
  },

  onUnload: function () {
    wx.removeStorageSync('pendingBuyProductId')
    wx.removeStorageSync('selectedAddressId')
  },

  loadDetail: function (id) {
    var that = this
    api.getProduct(id).then(function (p) {
      var meId = util.meId()
      that.setData({
        p: p,
        meId: meId,
        isOwn: !!(meId && p.sellerId == meId),
        activeImage: p.images && p.images[0] ? p.images[0].url : ''
      })
      // 详情接口对游客开放，登录用户另行查询收藏状态
      if (app.globalData.token && p.sellerId != meId) {
        api.checkFavorite(id).then(function (r) {
          that.setData({ 'p.isFavorited': !!(r && r.isFavorited) })
        }).catch(function () {})
      }
      that.loadComments(1, true)
    }).catch(function () {})
  },

  changeImage: function (e) { this.setData({ activeImage: e.currentTarget.dataset.url }) },

  // 全屏预览大图
  previewImage: function (e) {
    var p = this.data.p
    if (!p || !p.images || !p.images.length) return
    wx.previewImage({
      current: e.currentTarget.dataset.url || this.data.activeImage,
      urls: p.images.map(function (i) { return i.url })
    })
  },

  // 分享给微信好友（C2C 传播入口）
  onShareAppMessage: function () {
    var p = this.data.p
    if (!p) return {}
    return {
      title: p.title + ' 只要 ¥' + p.price + '，来看看！',
      path: '/pages/product/product?id=' + p.id,
      imageUrl: p.images && p.images[0] ? p.images[0].url : ''
    }
  },

  // ==================== 收藏 ====================
  toggleFav: function () {
    var that = this
    app.ensureLogin(function () {
      var p = that.data.p
      var fav = !!p.isFavorited
      var req = fav ? api.unFavorite(p.id) : api.toggleFavorite(p.id)
      req.then(function () {
        that.setData({
          'p.isFavorited': !fav,
          'p.favoriteCount': (p.favoriteCount || 0) + (fav ? -1 : 1)
        })
        wx.showToast({ title: fav ? '已取消收藏' : '收藏成功', icon: 'none' })
      }).catch(function () {})
    })
  },

  // ==================== 联系卖家（站内私信） ====================
  contactSeller: function () {
    var that = this
    var p = this.data.p
    app.ensureLogin(function () {
      api.getOrCreateConversation({ targetUserId: p.sellerId, productId: p.id }).then(function (conv) {
        wx.navigateTo({
          url: '/pages/chat/detail/detail?convId=' + conv.id +
               '&otherId=' + p.sellerId + '&otherName=' + encodeURIComponent(p.sellerName || '用户') +
               '&productId=' + p.id
        })
      }).catch(function () {})
    })
  },

  // ==================== 询价/砍价（购买意向） ====================
  openIntent: function () {
    var that = this
    app.ensureLogin(function () {
      that.setData({ intentSheet: true, intentMsg: '', intentPrice: String(that.data.p.price || '') })
    })
  },
  closeIntent: function () { this.setData({ intentSheet: false }) },
  onIntentMsg: function (e) { this.setData({ intentMsg: e.detail.value }) },
  onIntentPrice: function (e) { this.setData({ intentPrice: e.detail.value }) },
  noop: function () {},
  submitIntent: function () {
    var that = this
    var p = this.data.p
    var msg = this.data.intentMsg.trim()
    if (!msg) { wx.showToast({ title: '请填写留言', icon: 'none' }); return }
    this.setData({ sending: true })
    api.createIntent(p.id, {
      message: msg,
      expectedPrice: this.data.intentPrice ? Number(this.data.intentPrice) : null
    }).then(function () {
      that.setData({ sending: false, intentSheet: false })
      wx.showToast({ title: '已发送给卖家', icon: 'success' })
    }).catch(function () { that.setData({ sending: false }) })
  },

  // ==================== 下单 ====================

  buyNow: function () {
    var that = this
    var p = this.data.p
    app.ensureLogin(function () {
      if (that.data.isOwn) { wx.showToast({ title: '不能购买自己的商品', icon: 'none' }); return }
      if (p.status !== 1) { wx.showToast({ title: '商品当前不可购买', icon: 'none' }); return }
      wx.setStorageSync('pendingBuyProductId', p.id)
      wx.removeStorageSync('selectedAddressId')
      wx.navigateTo({ url: '/pages/address/address?select=1' })
    })
  },

  confirmOrder: function (pid, addrId) {
    var that = this
    var p = this.data.p
    var total = Number(p.price || 0) + Number(p.freightAmount || 0)
    wx.showModal({
      title: '确认下单',
      content: '商品 ¥' + p.price + ' + 运费 ¥' + (p.freightAmount || 0) + ' = ¥' + total.toFixed(2) + '\n使用余额支付？',
      success: function (r) {
        if (!r.confirm) return
        that.setData({ buying: true })
        var oid = null
        api.createOrder({ productId: pid, addressId: addrId }).then(function (order) {
          oid = order.orderId
          return api.payOrder(oid)
        }).then(function () {
          that.setData({ buying: false })
          wx.showToast({ title: '支付成功', icon: 'success' })
          setTimeout(function () {
            wx.redirectTo({ url: '/pages/order/detail/detail?id=' + oid })
          }, 600)
        }).catch(function () { that.setData({ buying: false }) })
      }
    })
  },

  // ==================== 卖家操作 ====================
  editProduct: function () {
    wx.navigateTo({ url: '/pages/publish/publish?id=' + this.data.p.id })
  },
  offProduct: function () {
    var that = this
    wx.showModal({
      title: '下架商品',
      content: '确定下架该商品吗？下架后其他用户将无法看到。',
      success: function (r) {
        if (!r.confirm) return
        api.offShelf(that.data.p.id).then(function () {
          wx.showToast({ title: '已下架', icon: 'success' })
          setTimeout(function () { wx.navigateBack() }, 600)
        }).catch(function () {})
      }
    })
  },

  // ==================== 评论 ====================
  loadComments: function (page, reset) {
    if (this.data.commentLoading) return
    var that = this
    var p = this.data.p
    if (!p || !p.id) return
    this.setData({ commentLoading: true })
    api.getComments({ productId: p.id, page: page, size: 20 }).then(function (res) {
      var list = res.records || []
      var merged = reset ? list : that.data.comments.concat(list)
      that.setData({
        comments: merged,
        commentPage: page + 1,
        hasMoreComment: merged.length < res.total,
        commentLoading: false
      })
    }).catch(function () { that.setData({ commentLoading: false }) })
  },
  loadMoreComments: function () {
    if (this.data.hasMoreComment && !this.data.commentLoading) {
      this.loadComments(this.data.commentPage, false)
    }
  },
  onCommentInput: function (e) { this.setData({ commentText: e.detail.value }) },
  submitComment: function () {
    var that = this
    var p = this.data.p
    var content = this.data.commentText.trim()
    if (!content) { wx.showToast({ title: '请输入评论内容', icon: 'none' }); return }
    if (this.data.commentSending) return
    app.ensureLogin(function () {
      that.setData({ commentSending: true })
      api.addComment({ productId: p.id, content: content }).then(function () {
        that.setData({ commentText: '', commentSending: false })
        wx.showToast({ title: '评论成功', icon: 'success' })
        that.loadComments(1, true)
      }).catch(function () { that.setData({ commentSending: false }) })
    })
  },
  deleteComment: function (e) {
    var that = this
    var id = e.currentTarget.dataset.id
    wx.showModal({
      title: '删除评论',
      content: '确定删除这条评论吗？',
      success: function (r) {
        if (!r.confirm) return
        api.deleteComment(id).then(function () {
          wx.showToast({ title: '已删除', icon: 'none' })
          that.loadComments(1, true)
        }).catch(function () {})
      }
    })
  },

  // ==================== 举报 ====================
  goReport: function () {
    var p = this.data.p
    var that = this
    app.ensureLogin(function () {
      if (that.data.isOwn) { wx.showToast({ title: '不能举报自己的商品', icon: 'none' }); return }
      wx.navigateTo({ url: '/pages/report/report?id=' + p.id + '&title=' + encodeURIComponent(p.title) })
    })
  },

  // ==================== 卖家主页 ====================
  goSellerHome: function () {
    var p = this.data.p
    if (!p || !p.sellerId) return
    wx.navigateTo({ url: '/pages/user/home/home?uid=' + p.sellerId })
  }
})
