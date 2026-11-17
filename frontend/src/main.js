import { createApp } from 'vue'
import { createPinia } from 'pinia'
import { ElLoading } from 'element-plus'
// 命令式组件样式按需手动引入（模板 el-* 组件由 unplugin 自动按需）
import 'element-plus/es/components/message/style/css'
import 'element-plus/es/components/message-box/style/css'
import 'element-plus/es/components/loading/style/css'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'
import App from './App.vue'
import router from './router'
import PageBackBar from './components/layout/PageBackBar.vue'
import ProductCover from './components/common/ProductCover.vue'
import ErrorPage from './components/common/ErrorPage.vue'
import SkeletonCard from './components/common/SkeletonCard.vue'
import LoadingSpinner from './components/common/LoadingSpinner.vue'
import './assets/styles/global.scss'
// Element Plus 暗色主题变量（html.dark 下激活）
import 'element-plus/theme-chalk/dark/css-vars.css'

const app = createApp(App)

// 全局渲染错误处理：生产环境打印错误（路由过渡组件若渲染抛错会白屏，日志便于定位）
app.config.errorHandler = (err, vm, info) => {
  console.error('[render-error]', info, err)
}

// 注册所有 Element Plus 图标
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component)
}

// 全局注册页面返回栏
app.component('PageBackBar', PageBackBar)
// 全局注册商品图片组件（无图/加载失败显示"暂无图片"占位）
app.component('ProductCover', ProductCover)
// 全局注册通用组件（错误页 / 骨架屏 / 加载动画）
app.component('ErrorPage', ErrorPage)
app.component('SkeletonCard', SkeletonCard)
app.component('LoadingSpinner', LoadingSpinner)

app.use(createPinia())
app.use(router)
// 注册 v-loading 指令（ElMessage/ElMessageBox 为直接调用，无需全局注册）
app.use(ElLoading)
app.mount('#app')
