import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'
import App from './App.vue'
import router from './router'
import PageBackBar from './components/layout/PageBackBar.vue'
import ProductCover from './components/common/ProductCover.vue'
import ErrorPage from './components/common/ErrorPage.vue'
import SkeletonCard from './components/common/SkeletonCard.vue'
import LoadingSpinner from './components/common/LoadingSpinner.vue'
import './assets/styles/global.scss'

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
app.use(ElementPlus)
app.mount('#app')
