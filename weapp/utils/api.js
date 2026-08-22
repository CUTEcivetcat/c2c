const request = require('./request.js')

module.exports = {
  // 商品
  getCategories: () => request.get('/product/category'),
  getProducts: (params) => request.get('/product/list', params),
  getProduct: (id) => request.get('/product/' + id),
  getProductsByIds: (ids) => request.get('/product/ids', { ids }),
  publish: (data) => request.post('/product', data),
  updateProduct: (id, data) => request.put('/product/' + id, data),
  offShelf: (id) => request.del('/product/' + id),
  getMyPublished: (params) => request.get('/product/my/published', params),
  toggleFavorite: (id) => request.post('/favorite/' + id),
  unFavorite: (id) => request.del('/favorite/' + id),
  getUserProducts: (uid, params) => request.get('/product/user/' + uid, params),

  // 订单（需登录）
  createOrder: (data) => request.post('/order', data),
  getOrder: (id) => request.get('/order/' + id),
  getBuyOrders: (params) => request.get('/order/list', params),
  getSellOrders: (params) => request.get('/order/sell/list', params),
  payOrder: (id) => request.post('/order/' + id + '/pay'),
  shipOrder: (id, data) => request.post('/order/' + id + '/ship', data),
  receiveOrder: (id) => request.post('/order/' + id + '/receive'),
  cancelOrder: (id, reason) => request.post('/order/' + id + '/cancel', { reason }),

  // 收藏
  getFavorites: (params) => request.get('/favorite/list', params),

  // 钱包
  walletProfile: () => request.get('/user/wallet/profile'),
  walletRecharge: (amount) => request.post('/user/wallet/recharge', { amount }),

  // 资料
  getProfile: () => request.get('/user/profile'),
  updateProfile: (data) => request.post('/user/profile?nickname=' + encodeURIComponent(data.nickname || '') + '&gender=' + (data.gender || 0)),
  upload: (filePath) => request.upload(filePath),

  // 账号
  bindEmail: (data) => request.post('/user/bind-email', data),
  sendSms: (account) => request.post('/user/sms/send?account=' + encodeURIComponent(account), {}),
  // 账号登录：loginType=1 密码 / =2 验证码，account 手机号或邮箱自动识别
  loginByPassword: (account, password) => request.post('/user/login', { account, password, loginType: 1 }),
  loginByCode: (account, smsCode) => request.post('/user/login', { account, smsCode, loginType: 2 }),

  // 地址
  getAddresses: () => request.get('/user/address'),
  getAddress: (id) => request.get('/user/address/' + id),
  addAddress: (data) => request.post('/user/address', data),
  updateAddress: (id, data) => request.put('/user/address/' + id, data),
  deleteAddress: (id) => request.del('/user/address/' + id),
  setDefaultAddress: (id) => request.put('/user/address/' + id + '/default')
}