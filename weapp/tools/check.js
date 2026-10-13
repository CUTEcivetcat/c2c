// 校园二手交易平台小程序 · 静态校验脚本
// 用法：node tools/check.js [weapp目录]（默认当前目录）
// 校验项：JS 语法、JSON 合法性、app.json 页面注册完整性、页面跳转 URL 有效性
const fs = require('fs')
const path = require('path')

const root = path.resolve(process.argv[2] || '.')
const jsFiles = []
const jsonFiles = []
;(function walk (d) {
  for (const f of fs.readdirSync(d)) {
    if (f === 'node_modules' || f === '.git') continue
    const p = path.join(d, f)
    const st = fs.statSync(p)
    if (st.isDirectory()) walk(p)
    else if (f.endsWith('.js')) jsFiles.push(p)
    else if (f.endsWith('.json')) jsonFiles.push(p)
  }
})(root)

let errors = 0
const fail = (msg) => { errors++; console.log('✗ ' + msg) }
const pass = (msg) => console.log('✓ ' + msg)

// 1. JS 语法
for (const f of jsFiles) {
  try { new Function(fs.readFileSync(f, 'utf8')) } catch (e) { fail('JS 语法错误 ' + path.relative(root, f) + ' → ' + e.message) }
}
pass('JS 语法：' + jsFiles.length + ' 个文件')

// 2. JSON 合法性
let appConfig = null
for (const f of jsonFiles) {
  try {
    const parsed = JSON.parse(fs.readFileSync(f, 'utf8'))
    if (path.basename(f) === 'app.json' && path.dirname(f) === root) appConfig = parsed
  } catch (e) { fail('JSON 解析错误 ' + path.relative(root, f) + ' → ' + e.message) }
}
pass('JSON 合法性：' + jsonFiles.length + ' 个文件')

// 3. 页面注册完整性
if (!appConfig) fail('未找到 app.json')
else {
  const pages = appConfig.pages || []
  let missing = 0
  for (const p of pages) {
    const base = path.join(root, p)
    for (const ext of ['.js', '.wxml', '.wxss', '.json']) {
      if (!fs.existsSync(base + ext)) { missing++; fail('页面文件缺失 ' + p + ext) }
    }
  }
  if (missing === 0) pass('页面注册：' + pages.length + ' 页文件齐全')
  const tabs = (appConfig.tabBar && appConfig.tabBar.list || []).map(t => t.pagePath)
  if (tabs.every(t => pages.includes(t))) pass('tabBar 页面均已注册')
  else fail('tabBar 页面未全部注册')
}

// 4. 跳转 URL 有效性
if (appConfig) {
  const pages = appConfig.pages || []
  let badUrls = 0
  const re = /(?:url|path)\s*:\s*(['"`])(\/[^'"`]*?)\1/g
  for (const f of jsFiles) {
    const s = fs.readFileSync(f, 'utf8')
    let m
    while ((m = re.exec(s))) {
      let u = m[2]
      if (u.startsWith('/pages/')) {
        u = u.split('?')[0].replace(/^\//, '')
        if (!pages.includes(u)) { badUrls++; fail('无效跳转 ' + path.relative(root, f) + ' → ' + u) }
      }
    }
  }
  if (badUrls === 0) pass('跳转 URL：全部有效')
}

console.log(errors === 0 ? '\n✅ 全部校验通过' : '\n❌ 共 ' + errors + ' 处错误')
process.exit(errors === 0 ? 0 : 1)
