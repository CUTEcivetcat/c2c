const request = require('./request.js')

module.exports = {
  // ==================== 公共 ====================
  getBanners: () => request.get('/banner/list'),
  getAnnouncements: (params) => request.get('/announcement/list', params),
  getAnnouncementLatest: (limit) => request.get('/announcement/latest', { limit: limit || 3 }),
  getAnnouncementForce: () => request.get('/announcement/force'),
  getAnnouncementPublish: () => request.get('/announcement/publish'),

  // ==================== 商品 ====================
  getCategories: () => request.get('/product/category'),
  getProducts: (params) => request.get('/product/list', params),
  getProduct: (id) => request.get('/product/' + id),
  getProductsByIds: (ids) => request.get('/product/ids', { ids }),
  publish: (data) => request.post('/product', data),
  updateProduct: (id, data) => request.put('/product/' + id, data),
  // 上架/下架等状态更新：目标状态放请求头 X-Status（1在售 4下架）
  updateProductStatus: (id, status) => request.put('/product/' + id + '/status', {}, { headers: { 'X-Status': status } }),
  offShelf: (id) => request.del('/product/' + id),
  getMyPublished: (params) => request.get('/product/my/published', params),
  getUserProducts: (uid, params) => request.get('/product/user/' + uid, params),

  // ==================== 收藏 ====================
  toggleFavorite: (id) => request.post('/favorite/' + id),
  unFavorite: (id) => request.del('/favorite/' + id),
  getFavorites: (params) => request.get('/favorite/list', params),
  checkFavorite: (id) => request.get('/favorite/check/' + id),

  // ==================== 订单 ====================
  createOrder: (data) => request.post('/order', data),
  getOrder: (id) => request.get('/order/' + id),
  getBuyOrders: (params) => request.get('/order/list', params),
  getSellOrders: (params) => request.get('/order/sell/list', params),
  payOrder: (id) => request.put('/order/' + id + '/pay'),
  shipOrder: (id, data) => request.put('/order/' + id + '/ship', data),
  receiveOrder: (id) => request.put('/order/' + id + '/receive'),
  cancelOrder: (id, reason) => request.put('/order/' + id + '/cancel', { reason }),

  // ==================== 购买意向（询价/砍价） ====================
  createIntent: (productId, data) => request.post('/product/intent/' + productId, data),
  getMyIntents: (params) => request.get('/product/intent/my', params),
  getSellerIntents: (params) => request.get('/product/intent/seller', params),
  replyIntent: (id, reply) => request.put('/product/intent/' + id + '/reply', { reply }),
  closeIntent: (id) => request.put('/product/intent/' + id + '/close'),
  dealIntent: (id) => request.put('/product/intent/' + id + '/deal'),

  // ==================== 商品评论 ====================
  getComments: (params) => request.get('/product/comment', params),
  addComment: (data) => request.post('/product/comment', data),
  deleteComment: (id) => request.del('/product/comment/' + id),

  // ==================== 交易评价 ====================
  submitRating: (data) => request.post('/rating', data),
  getOrderRatings: (orderId) => request.get('/rating/order/' + orderId),
  getUserRatings: (uid, params) => request.get('/rating/user/' + uid, params),

  // ==================== 站内私信 ====================
  getConversations: () => request.get('/im/conversation/list'),
  getOrCreateConversation: (data) => request.post('/im/conversation', data),
  getMessages: (conversationId, params) => request.get('/im/message/' + conversationId, params),
  sendMessage: (data) => request.post('/im/message', data),
  markRead: (conversationId) => request.put('/im/message/' + conversationId + '/read'),
  unreadCount: () => request.get('/im/unread/count'),
  unreadLatest: () => request.get('/im/unread/latest'),

  // ==================== 举报 ====================
  createReport: (data) => request.post('/report', data),
  getMyReports: (params) => request.get('/report/my', params),

  // ==================== 钱包 ====================
  walletProfile: () => request.get('/user/wallet/profile'),
  walletRecharge: (amount) => request.post('/user/wallet/recharge', { amount }),

  // ==================== 资料 ====================
  getProfile: () => request.get('/user/profile'),
  getUserPublicInfo: (uid) => request.get('/user/profile/' + uid),
  updateProfile: function (data) {
    var q = []
    if (data.nickname !== undefined && data.nickname !== null) q.push('nickname=' + encodeURIComponent(data.nickname))
    if (data.avatarUrl !== undefined && data.avatarUrl !== null) q.push('avatarUrl=' + encodeURIComponent(data.avatarUrl))
    if (data.gender !== undefined && data.gender !== null) q.push('gender=' + data.gender)
    if (data.bio !== undefined && data.bio !== null) q.push('bio=' + encodeURIComponent(data.bio))
    return request.put('/user/profile?' + q.join('&'), {})
  },
  upload: (filePath) => request.upload(filePath),

  // ==================== 账号 ====================
  bindEmail: (data) => request.post('/user/bind-email', data),
  bindPhone: (data) => request.post('/user/bind-phone', data),
  sendSms: (account) => request.post('/user/sms/send?account=' + encodeURIComponent(account), {}),
  // 账号登录：loginType=1 密码 / =2 验证码，account 手机号或邮箱自动识别
  loginByPassword: (account, password) => request.post('/user/login', { account, password, loginType: 1 }),
  loginByCode: (account, smsCode) => request.post('/user/login', { account, smsCode, loginType: 2 }),

  // ==================== 地址 ====================
  getAddresses: () => request.get('/user/address'),
  getAddress: (id) => request.get('/user/address/' + id),
  addAddress: (data) => request.post('/user/address', data),
  updateAddress: (id, data) => request.put('/user/address/' + id, data),
  deleteAddress: (id) => request.del('/user/address/' + id),
  setDefaultAddress: (id) => request.put('/user/address/' + id + '/default')
}
