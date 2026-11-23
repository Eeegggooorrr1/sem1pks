import { createApp } from 'vue'
import App from './App.vue'
import router from './router'
import { refreshSession } from './api/http'
import { session } from './session'
import './style.css'

async function start() {
  if (session.refreshToken) {
    try { await refreshSession() } catch {}
  }
  createApp(App).use(router).mount('#app')
}
void start()
