import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  base: '/zfh-virtual-device-frontend/',
  plugins: [vue()],
  define: {
    global: 'window'
  },
  server: {
    port: 5173,
    proxy: {
      '/zfh-virtual-device-backend': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        ws: true
      }
    }
  },
  build: {
    outDir: 'dist'
  }
})
