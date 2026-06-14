import { fileURLToPath, URL } from 'node:url'

import vue from '@vitejs/plugin-vue'
import { defineConfig, loadEnv } from 'vite'

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '')

  return {
    plugins: [vue()],
    resolve: {
      alias: {
        '@': fileURLToPath(new URL('./src', import.meta.url))
      }
    },
    build: {
      chunkSizeWarningLimit: 1100,
      rollupOptions: {
        output: {
          manualChunks(id) {
            const normalizedId = id.replace(/\\/g, '/')

            if (
              normalizedId.includes('/node_modules/echarts/') ||
              normalizedId.includes('/node_modules/zrender/') ||
              normalizedId.includes('/node_modules/vue-echarts/')
            ) {
              return 'charts'
            }

            if (
              normalizedId.includes('/node_modules/ant-design-vue/') ||
              normalizedId.includes('/node_modules/@ant-design/icons-vue/') ||
              normalizedId.includes('/node_modules/@ant-design/icons-svg/')
            ) {
              return 'ant-design'
            }

            if (
              normalizedId.includes('/node_modules/vue/') ||
              normalizedId.includes('/node_modules/@vue/') ||
              normalizedId.includes('/node_modules/vue-router/') ||
              normalizedId.includes('/node_modules/pinia/')
            ) {
              return 'vue-vendor'
            }

            if (normalizedId.includes('/node_modules/axios/')) {
              return 'http'
            }
          }
        }
      }
    },
    server: {
      port: Number(env.VITE_DEV_PORT || 5173),
      proxy: {
        '/api': {
          target: env.VITE_API_BASE_URL || 'http://localhost:8080',
          changeOrigin: true
        },
        '/actuator': {
          target: env.VITE_API_BASE_URL || 'http://localhost:8080',
          changeOrigin: true
        }
      }
    }
  }
})
