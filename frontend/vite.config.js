import { defineConfig } from 'vitest/config'
import { loadEnv } from 'vite'
import vue from '@vitejs/plugin-vue'
import Components from 'unplugin-vue-components/vite'
import { ElementPlusResolver } from 'unplugin-vue-components/resolvers'

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
      changeOrigin: true,
      // 后端有 context-path /api/v1，开发代理需重写（生产由 nginx 直接映射 /files 无此问题）
      rewrite: p => p.replace(/^\/files/, '/api/v1/files')
    }
  }

  const isTest = process.env.VITEST === 'true'

  return {
    plugins: [
      vue(),
      // element-plus 组件按需自动引入（模板中的 el-* 组件及其样式）；测试环境不挂，避免 resolver 注入 CSS 被 jsdom 解析失败
      ...(isTest ? [] : [Components({ resolvers: [ElementPlusResolver()] })])
    ],
    test: {
      environment: 'jsdom',
      globals: true,
      // 聊天方向回归等组件测试
      include: ['src/**/*.spec.js']
    },
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
      // element-plus 为全量引入（含全部图标），单块客观体积较大；按需引入需接 unplugin-vue-components，列为后续瘦身项
      chunkSizeWarningLimit: 1100,
      rollupOptions: {
        output: {
          // 大依赖独立分包，提升首屏加载与缓存命中
          manualChunks(id) {
            if (!id.includes('node_modules')) return
            if (id.includes('@element-plus/icons-vue')) return 'icons'
            if (/[\\/]node_modules[\\/](vue|vue-router|pinia|axios)[\\/]/.test(id)) return 'vendor'
          }
        }
      }
    }
  }
})
