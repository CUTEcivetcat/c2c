import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'
import App from './App.vue'
import router from './router'
import PageBackBar from './components/layout/PageBackBar.vue'
import ProductCover from './components/common/ProductCover.vue'
import './assets/styles/global.scss'

const app = createApp(App)

// 注册所有 Element Plus 图标
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component)
}

// 全局注册页面返回栏
app.component('PageBackBar', PageBackBar)
// 全局注册商品图片组件（无图/加载失败显示"暂无图片"占位）
app.component('ProductCover', ProductCover)

app.use(createPinia())
app.use(router)
app.use(ElementPlus)
app.mount('#app')
