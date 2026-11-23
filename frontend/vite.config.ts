import { defineConfig, loadEnv } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig(({ command, mode }) => {
  const env = loadEnv(mode, '..', '')
  if (command === 'serve' && !env.BACKEND_PORT) throw new Error('Укажите BACKEND_PORT в корневом .env')
  return {
    plugins: [vue()],
    server: { proxy: { '/api': { target: `http://127.0.0.1:${env.BACKEND_PORT}` } } },
  }
})
