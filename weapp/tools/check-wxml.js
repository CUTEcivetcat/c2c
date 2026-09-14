// 闲小鱼小程序 · WXML 绑定一致性检查
// 1) bindtap/catchtap/bindinput 等 handler 是否在对应 page.js 中存在（防"点击没反应"）
// 2) data-url 跳转目标是否为已注册页面（防 navigateTo 静默失败）
// 3) wxml 引用的 class 是否在对应 wxss 中定义（防"显示不完全"）
// 用法：node tools/check-wxml.js [weapp目录]
const fs = require('fs')
const path = require('path')

const root = path.resolve(process.argv[2] || '.')
const app = JSON.parse(fs.readFileSync(path.join(root, 'app.json'), 'utf8'))
const pages = app.pages || []
let errors = 0
const fail = (m) => { errors++; console.log('✗ ' + m) }

function walk (d, cb) {
  for (const f of fs.readdirSync(d)) {
    const p = path.join(d, f)
    const st = fs.statSync(p)
    if (st.isDirectory()) walk(p, cb)
    else cb(p)
  }
}

const wxmls = []
walk(root, (p) => { if (p.endsWith('.wxml')) wxmls.push(p) })

// 收集 app.wxss 全局类名（页面 wxss 未定义时，可能是全局样式）
// 采用 token 级匹配：`.a.b`、`.btn.mini` 等组合选择器中的每个类名都计入
function collectClasses(css) {
  const set = new Set()
  const re = /\.([A-Za-z_][\w-]*)/g
  let m
  while ((m = re.exec(css))) set.add(m[1])
  return set
}
const globalClasses = collectClasses(fs.readFileSync(path.join(root, 'app.wxss'), 'utf8'))

for (const w of wxmls) {
  const rel = path.relative(root, w).replace(/\\/g, '/')
  const js = w.replace(/\.wxml$/, '.js')
  let code = ''
  try { code = fs.readFileSync(js, 'utf8') } catch (e) {
    fail(rel + ' → 缺少同名 JS')
    continue
  }
  const src = fs.readFileSync(w, 'utf8')
  let m

  // 1) 事件 handler 检查
  const handlerRe = /(?:bind|catch)(?:tap|input|change|confirm|scrolltoupper|scrolltolower|longpress|submit|blur|focus|touchstart|touchend)\s*=\s*"([A-Za-z_$][\w$]*)"/g
  const seen = {}
  while ((m = handlerRe.exec(src))) {
    const name = m[1]
    if (seen[name]) continue
    seen[name] = 1
    if (name === 'noop') continue
    const re = new RegExp('(?:^|\\s)' + name + '\\s*:', 'm')
    if (!re.test(code) && !new RegExp(name + '\\s*\\(').test(code)) {
      fail(rel + ' → 事件绑定 ' + name + ' 在 JS 中未定义')
    }
  }

  // 2) data-url 检查
  const urlRe = /data-url\s*=\s*"([^"]+)"/g
  while ((m = urlRe.exec(src))) {
    let u = m[1]
    if (u.startsWith('/pages/')) {
      const base = u.split('?')[0].replace(/^\//, '')
      if (!pages.includes(base)) fail(rel + ' → data-url 未注册页面：' + u)
    }
  }

  // 3) class 存在性检查（wxml 引用的类在对应 wxss 或 app.wxss 中定义；组合选择器也计入）
  const wxss = w.replace(/\.wxml$/, '.wxss')
  let cssCode = ''
  try { cssCode = fs.readFileSync(wxss, 'utf8') } catch (e) {}
  const pageClasses = cssCode ? collectClasses(cssCode) : new Set()
  if (cssCode) {
    const clsRe = /class\s*=\s*"([^"]+)"/g
    while ((m = clsRe.exec(src))) {
      // 剔除 {{...}} 模板表达式后取真实 class token
      const clean = m[1].replace(/\{\{[^}]*\}\}/g, ' ')
      const classes = clean.split(/\s+/).filter(Boolean)
      for (const c of classes) {
        if (!/^[a-z][\w-]*$/.test(c)) continue
        if (/^(sk-box|glass|glass-card|page|fade-up|on|off|up|down|active|safe-bottom|hidden)$/.test(c)) continue
        if (globalClasses.has(c) || pageClasses.has(c)) continue
        fail(rel + ' → class 未定义（页面 wxss 与 app.wxss 均无）：.' + c)
      }
    }
  }
}

console.log(errors === 0 ? '✅ WXML 绑定检查全部通过' : '\n❌ 共 ' + errors + ' 处问题')
process.exit(errors === 0 ? 0 : 1)
