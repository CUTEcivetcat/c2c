// 统一请求封装
// 端点前缀统一取 app.globalData.serverUrl,登录/上传使用同一 base。
// 后端上传/返回的图片地址是相对路径 /files/xxx，这里统一补全为服务器绝对地址。

function baseUrl() {
  const app = getApp()
  return (app && app.globalData && app.globalData.serverUrl) || 'http://81.71.118.128/api/v1'
}

// 服务器「源」，如 http://81.71.118.128（去掉 /api/v1 后缀）
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
      if (k === 'url' && typeof data[k] === 'string' && data[k].indexOf('/files/') === 0) {
        out[k] = origin() + data[k]
      }
    })
    return out
  }
  return data
}

function request(method, url, data, showErr) {
  return new Promise(function (resolve, reject) {
    const app = getApp()
    const token = (app && app.globalData && app.globalData.token) || ''
    wx.request({
      url: baseUrl() + url,
      method: method,
      data: data,
      header: { 'Content-Type': 'application/json', 'Authorization': 'Bearer ' + token },
      success: function (res) {
        var body = res.data
        if (body && body.code === 200) resolve(patchImages(body.data))
        else {
          var msg = (body && body.message) || '请求失败'
          if (showErr !== false) wx.showToast({ title: msg, icon: 'none' })
          reject(new Error(msg))
        }
      },
      fail: function () {
        if (showErr !== false) wx.showToast({ title: '网络异常', icon: 'none' })
        reject(new Error('network'))
      }
    })
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
  get: function (u, d) { return request('GET', u, d) },
  post: function (u, d) { return request('POST', u, d) },
  put: function (u, d) { return request('PUT', u, d) },
  del: function (u) { return request('DELETE', u) },
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