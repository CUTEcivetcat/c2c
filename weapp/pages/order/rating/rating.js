const api = require('../../../utils/api.js')

const TAGS = ['守时', '商品如描述', '沟通顺畅', '发货快', '态度好', '包装用心']

Page({
  data: {
    orderId: null,
    role: 1, // 1 买家评卖家 / 2 卖家评买家
    ratedUserId: null,
    orderNo: '',
    title: '',
    score: 5,
    comment: '',
    tags: [],
    submitting: false,
    tagOptions: TAGS
  },

  onLoad: function (opt) {
    this.setData({
      orderId: Number(opt.orderId),
      role: Number(opt.role) || 1,
      ratedUserId: Number(opt.ratedUserId),
      orderNo: opt.orderNo || '',
      title: opt.title || ''
    })
  },

  setScore: function (e) {
    this.setData({ score: Number(e.currentTarget.dataset.s) })
  },

  onComment: function (e) { this.setData({ comment: e.detail.value }) },

  toggleTag: function (e) {
    var tag = e.currentTarget.dataset.t
    var tags = this.data.tags
    var idx = tags.indexOf(tag)
    if (idx >= 0) tags.splice(idx, 1)
    else if (tags.length < 4) tags.push(tag)
    else { wx.showToast({ title: '最多选4个标签', icon: 'none' }); return }
    this.setData({ tags: tags })
  },

  submit: function () {
    var that = this
    var d = this.data
    if (d.submitting) return
    this.setData({ submitting: true })
    api.submitRating({
      orderId: d.orderId,
      role: d.role,
      ratedUserId: d.ratedUserId,
      score: d.score,
      comment: d.comment.trim(),
      tags: JSON.stringify(d.tags)
    }).then(function () {
      wx.showToast({ title: '评价成功', icon: 'success' })
      setTimeout(function () {
        wx.navigateBack()
      }, 700)
    }).catch(function () {
      that.setData({ submitting: false })
    })
  }
})
