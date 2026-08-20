import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  // 部署在子路径 /blog/ 下，所有资源引用都以 /blog/ 开头
  base: '/blog/',
  plugins: [vue()],
  resolve: {
    alias: { '@': '/src' }
  },
  server: {
    port: 5175,
    open: true
  }
})
