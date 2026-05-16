import { defineConfig, loadEnv } from 'vite'
import vue from '@vitejs/plugin-vue'

// https://vite.dev/config/
export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '')
  const proxyTarget = env.VITE_PROXY_TARGET || 'http://localhost:8080'

  return {
    base: '/battery-quote-center/',
    plugins: [vue()],
    build: {
      outDir: '../src/main/resources/static/battery-quote-center',
      emptyOutDir: true,
    },
    server: {
      proxy: {
        '/battery-quote-center/api': {
          target: proxyTarget,
          changeOrigin: true,
        },
      },
    },
  }
})
