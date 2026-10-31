// 统一请求封装
// 端点前缀统一取 app.globalData.serverUrl,登录/上传使用同一 base。
// 后端上传/返回的图片地址是相对路径 /files/xxx，这里统一补全为服务器绝对地址。
// 401 统一处理：清除本地登录态并引导回"我的"重新登录。

function baseUrl() {
  const app = getApp()
  return (app && app.globalData && app.globalData.serverUrl) || 'http://192.168.1.11:8080/api/v1'
}

// 服务器「源」，如 http://192.168.1.11:8080（去掉 /api/v1 后缀）
function origin() {
  const base = baseUrl()
  const idx = base.indexOf('/api')
  return idx > 0 ? base.substring(0, idx) : base
}

// 相对路径 /files/... 转为绝对地址
function absoluteUrl(v) {
  if (typeof v !== 'string') return v
  if (v.indexOf('/files/') === 0) return origin() + v
  return v
}

// 递归把 data 对象/数组里所有 /files/ 开头的地址补全为绝对地址
function patchImages(data) {
  if (Array.isArray(data)) return data.map(patchImages)
  if (data && typeof data === 'object') {
    const out = {}
    Object.keys(data).forEach(function (k) {
      out[k] = patchImages(data[k])
    })
    return out
  }
  return absoluteUrl(data)
}

// 登录态失效：清空本地并跳回我的页
function handle401() {
  const app = getApp()
  if (app && app.globalData) {
    app.globalData.token = ''
    app.globalData.userInfo = null
  }
  wx.removeStorageSync('token')
  wx.removeStorageSync('userInfo')
  wx.showToast({ title: '登录已过期，请重新登录', icon: 'none' })
  setTimeout(function () {
    wx.switchTab({ url: '/pages/mine/mine' })
  }, 600)
}

function request(method, url, data, opts) {
  opts = opts || {}
  return new Promise(function (resolve, reject) {
    const app = getApp()
    const token = (app && app.globalData && app.globalData.token) || ''
    const header = { 'Content-Type': 'application/json' }
    if (opts.headers) {
      Object.keys(opts.headers).forEach(function (k) { header[k] = opts.headers[k] })
    }
    if (token) header['Authorization'] = 'Bearer ' + token

    // 弱网自动重试一次（仅网络层失败，不重试业务错误）
    var attempts = 0
    function doRequest() {
      attempts++
      wx.request({
        url: baseUrl() + url,
        method: method,
        data: data,
        header: header,
        success: function (res) {
          var body = res.data
          if (body && body.code === 200) {
            resolve(patchImages(body.data))
          } else if (body && (body.code === 401 || res.statusCode === 401)) {
            if (token) handle401()
            else if (opts.showErr !== false) wx.showToast({ title: (body && body.message) || '请先登录', icon: 'none' })
            reject(new Error((body && body.message) || 'unauthorized'))
          } else {
            var msg = (body && body.message) || '请求失败'
            if (opts.showErr !== false) wx.showToast({ title: msg, icon: 'none' })
            reject(new Error(msg))
          }
        },
        fail: function () {
          // 仅读请求（GET）弱网自动重试一次，写请求不重试避免重复下单/支付
          if (method === 'GET' && attempts < 2) { doRequest() }
          else {
            if (opts.showErr !== false) wx.showToast({ title: '网络异常', icon: 'none' })
            reject(new Error('network'))
          }
        }
      })
    }
    doRequest()
  })
}

function login(data) {
  return new Promise(function (resolve, reject) {
    wx.request({
      url: baseUrl() + '/user/wechat-login',
      method: 'POST',
      data: data,
      header: { 'Content-Type': 'application/json' },
      success: function (res) {
        var body = res.data
        if (body && body.code === 200) resolve(patchImages(body.data))
        else reject(new Error((body && body.message) || 'login failed'))
      },
      fail: reject
    })
  })
}

module.exports = {
  baseUrl: baseUrl,
  get: function (u, d, o) { return request('GET', u, d, o) },
  post: function (u, d, o) { return request('POST', u, d, o) },
  put: function (u, d, o) { return request('PUT', u, d, o) },
  del: function (u, d, o) { return request('DELETE', u, d, o) },
  login: login,
  upload: function (filePath) {
    var token = (getApp() && getApp().globalData && getApp().globalData.token) || ''
    return new Promise(function (resolve, reject) {
      wx.uploadFile({
        url: baseUrl() + '/upload/image',
        filePath: filePath,
        name: 'file',
        header: { Authorization: 'Bearer ' + token },
        success: function (r) {
          var body = JSON.parse(r.data)
          if (body.code === 200) resolve(patchImages(body.data).url)
          else reject(new Error(body.message))
        },
        fail: function (e) { reject(e) }
      })
    })
  }
}
