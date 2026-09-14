const api = require('../../utils/api.js')

// 本地分类图标映射（后端无 iconUrl 时兜底，风格统一）
const CAT_ICONS = {
  '书籍': '/images/category/book.jpg',
  '数码': '/images/category/digital.jpg',
  '手机': '/images/category/phone.jpg',
  '日用': '/images/category/daily.jpg',
  '教材': '/images/category/textbook.jpg',
  '英语': '/images/category/english.jpg',
  '笔记本电脑': '/images/category/laptop.jpg',
  '苹果手机': '/images/category/apple-phone.jpg',
  '宿舍用品': '/images/category/dorm.jpg'
}

Page({
  data: { categories: [], activeId: 0, products: [], loading: false },

  catIcon: function (name) { return CAT_ICONS[name] || '' },

  onLoad: function (opt) {
    this.preselectId = opt && opt.id ? Number(opt.id) : 0
    this.loadCategories()
  },

  loadCategories: function () {
    var that = this
    api.getCategories().then(function (cats) {
      that.setData({ categories: cats || [] })
      if (cats && cats.length) {
        // 支持从首页分类入口预选
        var idx = 0
        if (that.preselectId) {
          for (var i = 0; i < cats.length; i++) {
            if (cats[i].id === that.preselectId) { idx = i; break }
          }
        }
        that.select(idx)
      }
    })
  },

  // 兼容两种调用：bindtap 传事件对象 / 内部传数字 index
  select: function (e) {
    var idx = typeof e === 'number' ? e : Number(e.currentTarget.dataset.index)
    var cat = this.data.categories[idx]
    if (!cat) return
    this.setData({ activeId: idx })
    this.load(cat.id)
  },

  load: function (cid) {
    var that = this
    this.setData({ loading: true })
    api.getProducts({ categoryId: cid, page: 1, size: 50, sort: 'hot' }).then(function (res) {
      that.setData({ products: res.records || [], loading: false })
    }).catch(function () { that.setData({ loading: false }) })
  },

  goProduct: function (e) { wx.navigateTo({ url: '/pages/product/product?id=' + e.currentTarget.dataset.id }) }
})
