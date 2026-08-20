import { defineConfig, loadEnv } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '')
  const serverOrigin = env.VITE_SERVER_ORIGIN || 'http://127.0.0.1:8080'

  return {
    plugins: [vue()],
    server: {
      port: 5173,
      open: true,
      proxy: {
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
    },
    resolve: {
      alias: {
        '@': '/src'
      }
    }
  }
})
