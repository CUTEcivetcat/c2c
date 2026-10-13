// 青禾集市小程序 · 页面逻辑冒烟测试
// mock 微信环境（wx/Page/App/getApp），加载全部页面 JS，
// 以"已登录 / 未登录"两种状态调用生命周期与核心方法，捕获运行时异常。
// 用法：node tools/smoke-test.js [weapp目录]
const fs = require('fs')
const path = require('path')

const root = path.resolve(process.argv[2] || '.')

// ==================== mock 微信环境 ====================
const storage = {}
const calls = []
const userInfo = { id: 1, nickname: '测试用户', avatarUrl: '', gender: 0, bio: '', loginSource: 'wechat' }

function mockApp() {
  return {
    globalData: { token: 'mock-token', userInfo: userInfo, serverUrl: 'http://mock-server/api/v1' },
    ensureLogin: function (cb) { if (cb) cb() },
    wechatLogin: function (cb) { if (cb) cb(null, { token: 'mock-token', userInfo: userInfo }) },
    refreshUserInfo: function (cb) { if (cb) cb(userInfo) },
    checkForceAnnouncement: function () { return Promise.resolve(null) },
    maybeShowForceAnnouncement: function () {},
    saveLogin: function (d) { this.globalData.token = d.token; this.globalData.userInfo = d.userInfo },
    clearLogin: function () { this.globalData.token = ''; this.globalData.userInfo = null },
    markForceAnnouncementDismissed: function () {}
  }
}

let wxMock = null
function makeWx(loggedIn) {
  const app = mockApp()
  if (!loggedIn) { app.globalData.token = ''; app.globalData.userInfo = null }
  const routes = []
  return {
    app: app,
    getStorageSync: (k) => (k in storage ? storage[k] : ''),
    setStorageSync: (k, v) => { storage[k] = v },
    removeStorageSync: (k) => { delete storage[k] },
    showToast: () => {},
    hideToast: () => {},
    showLoading: () => {},
    hideLoading: () => {},
    showModal: (o) => { if (o && o.success) o.success({ confirm: true, content: '' }) },
    showActionSheet: (o) => { if (o && o.success) o.success({ tapIndex: 0 }) },
    navigateTo: (o) => { if (o) calls.push(['navigateTo', o.url]); if (o && o.fail) o.fail() },
    navigateBack: (o) => { if (o && o.fail) o.fail() },
    redirectTo: (o) => { if (o) calls.push(['redirectTo', o.url]) },
    switchTab: (o) => { if (o) calls.push(['switchTab', o.url]) },
    setClipboardData: (o) => { if (o && o.success) o.success() },
    setTabBarBadge: () => {},
    removeTabBarBadge: (o) => { if (o && o.fail) o.fail() },
    setNavigationBarTitle: () => {},
    previewImage: () => {},
    pageScrollTo: () => {},
    stopPullDownRefresh: () => {},
    chooseImage: (o) => { if (o && o.success) o.success({ tempFilePaths: ['/mock/a.jpg'] }) },
    login: (o) => { if (o && o.success) o.success({ code: 'mock-code' }) },
    // 网络：成功响应（默认空列表），支持按 url 定制
    request: (o) => {
      const body = { code: 200, message: 'ok', data: mockDataFor(o.url, o.method) }
      if (o.success) o.success({ statusCode: 200, data: body })
    },
    uploadFile: (o) => {
      if (o.success) o.success({ statusCode: 200, data: JSON.stringify({ code: 200, data: { url: '/files/mock.jpg' } }) })
    }
  }
}

// 按接口返回合理的 mock 数据，覆盖主链路字段
function mockDataFor(url, method) {
  const emptyPage = { records: [], total: 0, page: 1, size: 20 }
  if (/\/category/.test(url)) return []
  if (/\/banner/.test(url)) return []
  if (/\/product\/list|my\/published|user\/\d+|favorite\/list|order\/list|order\/sell|product\/comment|product\/intent|announcement|rating\/user|report\/my|conversation|message\/|unread/.test(url)) {
    return emptyPage
  }
  if (/\/favorite\/check/.test(url)) return { isFavorited: false }
  if (/\/user\/profile/.test(url)) return userInfo
  if (/\/wallet\/profile/.test(url)) return { balance: '100.00', logs: [] }
  if (/\/user\/address/.test(url) && !/\/user\/address\/\d+/.test(url)) return []
  if (/\/user\/address\/\d+$/.test(url)) return { id: 1, receiverName: '张三', phone: '13800000000', province: '河南省', city: '郑州市', district: '中原区', detail: 'xx路1号', isDefault: 1 }
  if (/\/rating\/order/.test(url)) return { buyerRating: null, sellerRating: null }
  if (/\/announcement\/(latest|force|publish)/.test(url)) return []
  if (/\/product\/\d+$/.test(url)) {
    return {
      id: 1, title: '测试商品', price: 99, originalPrice: 199, description: '描述',
      condition: 1, conditionText: '全新', freightType: 0, freightAmount: 0,
      location: '郑州', categoryId: 1, categoryName: '数码', sellerId: 2,
      sellerName: '卖家', sellerAvatar: '', sellerReputation: '5.0',
      viewCount: 10, favoriteCount: 3, status: 1,
      images: [{ id: 1, url: '/files/p1.jpg' }], createdAt: '2026-09-13 10:00:00'
    }
  }
  if (/\/product\/ids/.test(url)) return []
  if (/\/order\/\d+$/.test(url)) {
    return {
      id: 1, orderNo: 'C2C20260913001', buyerId: 1, sellerId: 2, productId: 1,
      productTitle: '测试商品', productImage: '/files/p1.jpg',
      price: 99, freightAmount: 0, totalAmount: 99, status: 0,
      addressSnapshot: '{"receiverName":"张三","phone":"13800000000","province":"河南省","city":"郑州市","district":"中原区","detail":"xx路1号"}',
      paymentMethod: 'balance', createdAt: '2026-09-13 10:00:00'
    }
  }
  if (/\/im\/conversation\/list/.test(url)) return []
  if (/\/im\/conversation$/.test(url)) return { id: 1 }
  if (/\/im\/message\/\d+/.test(url)) return { records: [], total: 0 }
  if (/\/im\/unread\//.test(url)) return { unreadTotal: 0 }
  return {}
}

// ==================== 页面加载与实例化 ====================
let failures = 0
let loaded = 0
const pass = (m) => console.log('✓ ' + m)
const fail = (m) => { failures++; console.log('✗ ' + m) }

function loadPages() {
  const defs = {}
  const pageFiles = []
  ;(function walk(d) {
    for (const f of fs.readdirSync(d)) {
      const p = path.join(d, f)
      const st = fs.statSync(p)
      if (st.isDirectory()) walk(p)
      else if (f.endsWith('.js') && /pages[\\/]/.test(p.replace(root, '')) && !f.startsWith('_')) pageFiles.push(p)
    }
  })(root)
  // 页面模块顶层常调用 getApp()，加载阶段先提供默认实例
  global.getApp = () => mockApp()
  pageFiles.forEach((p) => {
    let lastDef = null
    global.Page = (def) => { lastDef = def }
    try {
      delete require.cache[require.resolve(p)]
      require(p)
    } catch (e) {
      fail('加载失败 ' + path.relative(root, p).replace(/\\/g, '/') + ' → ' + e.message)
      return
    }
    if (!lastDef) { fail('未注册 Page ' + path.relative(root, p)); return }
    const key = path.relative(root, p).replace(/\\/g, '/').replace(/\.js$/, '')
    defs[key] = { file: p, def: lastDef }
    loaded++
  })
  return defs
}

function setPath(obj, path, val) {
  const parts = path.split('.')
  let cur = obj
  for (let i = 0; i < parts.length - 1; i++) {
    if (cur[parts[i]] === undefined) cur[parts[i]] = {}
    cur = cur[parts[i]]
  }
  cur[parts[parts.length - 1]] = val
}

function instantiate(def) {
  const inst = Object.assign({}, def)
  inst.data = JSON.parse(JSON.stringify(def.data || {}))
  inst.setData = function (patch, cb) {
    Object.keys(patch).forEach((k) => setPath(this.data, k, patch[k]))
    if (cb) cb()
    return this.data
  }
  return inst
}

async function flush(ms) {
  await new Promise((r) => setTimeout(r, ms || 60))
}

async function runPage(defs, key, loggedIn) {
  const wx = makeWx(loggedIn)
  global.wx = wx
  global.getApp = () => wx.app
  const inst = instantiate(defs[key].def)
  const rel = key
  const methods = Object.keys(defs[key].def)
  try {
    // onLoad（带参数）
    if (typeof inst.onLoad === 'function') {
      const opt = { id: '1', uid: '2', opt: { id: '1' }, orderId: '1', role: '1', ratedUserId: '2', convId: '1', otherId: '2', otherName: 'a', tab: 'buy', select: '1', type: '1' }
      try { inst.onLoad(opt) } catch (e) { fail(rel + ' → onLoad 抛错：' + e.message) }
      await flush()
    }
    // onShow
    if (typeof inst.onShow === 'function') {
      try { inst.onShow() } catch (e) { fail(rel + ' → onShow 抛错：' + e.message) }
      await flush()
    }
    // 核心数据方法（生命周期 + 业务动作 + 数据加载）
    const core = methods.filter((m) => /^(onPullDownRefresh|load[A-Z]|reload|refresh|search|toggle|apply|submit|send|pay|ship|receive|cancel|deal|close|reply|choose|save|remove|off|onShelf|buyNow|contact|goProduct|goRate|dismiss|logout|bindEmail|sendCode|wechatLogin|phoneLogin|codeLogin|doLogin|getWxPhone|onGetPhone|copyOrderNo|preview|openImg|goBuy|goChat|like|favorite|collect|rate|publish|update|del|edit|add|select|switch|setDefault|deleteProduct|markRead|inputMsg|sendMsg|getMore|loadMore|applyPendingFill|syncCatName|onCategory|onPicker|previewImage)$/.test(m))
    for (const m of core) {
      try {
        const ev = { currentTarget: { dataset: { id: 1, index: 0, url: '/files/p1.jpg', t: 1, s: 5, k: '1', w: '手机', c: {} } }, detail: { value: '测试' } }
        const r = inst[m](ev)
        if (r && typeof r.then === 'function') await r
        await flush()
      } catch (e) {
        fail(rel + ' → 方法 ' + m + ' 抛错：' + e.message)
      }
    }
    pass(rel + (loggedIn ? '（已登录）' : '（未登录）'))
  } catch (e) {
    fail(rel + ' → 生命周期抛错：' + e.message)
  }
}

async function main() {
  const defs = loadPages()
  console.log('共加载 ' + loaded + ' 个页面')
  for (const key of Object.keys(defs)) {
    await runPage(defs, key, true)
    await runPage(defs, key, false)
  }
  console.log(failures === 0 ? '\n✅ 冒烟测试全部通过' : '\n❌ ' + failures + ' 处失败')
  process.exit(failures === 0 ? 0 : 1)
}

main()
