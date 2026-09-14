import { defineConfig, loadEnv } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '')
  const serverOrigin = env.VITE_SERVER_ORIGIN || 'http://127.0.0.1:8080'

  const proxy = {
    '/api': {
      target: serverOrigin,
      changeOrigin: true
    },
    '/uploads': {
      target: serverOrigin,
      changeOrigin: true
    },
    '/files': {
      target: serverOrigin,
      changeOrigin: true
    }
  }

  return {
    plugins: [vue()],
    server: {
      port: 5173,
      open: true,
      proxy
    },
    preview: {
      port: 4173,
      proxy
    },
    resolve: {
      alias: {
        '@': '/src'
      }
    },
    build: {
      rollupOptions: {
        output: {
          // 大依赖独立分包，提升首屏加载与缓存命中
          manualChunks: {
            'element-plus': ['element-plus'],
            vendor: ['vue', 'vue-router', 'pinia', 'axios']
          }
        }
      }
    }
  }
})
