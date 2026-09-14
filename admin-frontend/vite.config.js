import { defineConfig, loadEnv } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '')
  const serverOrigin = env.VITE_SERVER_ORIGIN || 'http://127.0.0.1:8080'

  return {
    base: '/admin/',
    plugins: [vue()],
    resolve: {
      alias: {
        '@': '/src'
      }
    },
    server: {
      port: 5174,
      open: env.VITE_OPEN === 'true' || env.VITE_OPEN === '1',
      proxy: {
        '/api': { target: serverOrigin, changeOrigin: true }
      }
    },
    build: {
      rollupOptions: {
        output: {
          // 大依赖独立分包，提升首屏加载与缓存命中
          manualChunks: {
            'element-plus': ['element-plus'],
            echarts: ['echarts'],
            vendor: ['vue', 'vue-router', 'pinia', 'axios']
          }
        }
      }
    }
  }
})
