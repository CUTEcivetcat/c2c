const app = getApp()

Page({
  data: {
    products: [],
    loading: true
  },

  onLoad() {
    this.loadProducts()
  },

  loadProducts() {
    const url = app.globalData.serverUrl + '/product/list?page=1&size=20&sort=created_at'
    wx.request({
      url,
      success: (res) => {
        if (res.data.code === 200) {
          this.setData({ products: res.data.data.records || [], loading: false })
        }
      }
    })
  }
})