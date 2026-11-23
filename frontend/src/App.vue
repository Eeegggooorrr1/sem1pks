<script setup lang="ts">
import { watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { session } from './session'
import { accounts } from './api/accounts'
import { useTask } from './composables/useTask'
import ErrorBox from './components/ErrorBox.vue'
const route = useRoute(), router = useRouter()
const { busy, error, run } = useTask()
watch(() => session.account, account => {
  if (!account && (route.meta.auth || route.meta.admin)) void router.replace({ path: '/login', query: { next: route.fullPath } })
  else if (route.meta.admin && account?.role !== 'ADMIN') void router.replace('/')
})
async function logout() { await run(async () => { await accounts.logout(); await router.push('/') }) }
</script>
<template>
  <a class="skip-link" href="#content">К содержимому</a>
  <header class="app-header">
    <RouterLink class="app-name" to="/">Объявления</RouterLink>
    <nav aria-label="Основная навигация">
      <RouterLink to="/" exact-active-class="current">Каталог</RouterLink>
      <template v-if="session.account">
        <RouterLink to="/orders/outgoing" active-class="current">Покупки</RouterLink>
        <RouterLink to="/orders/incoming" active-class="current">Продажи</RouterLink>
      </template>
      <template v-if="session.account?.role === 'ADMIN'">
        <RouterLink to="/analytics" active-class="current">Аналитика</RouterLink>
        <RouterLink to="/categories" active-class="current">Категории</RouterLink>
        <RouterLink to="/users" active-class="current">Пользователи</RouterLink>
        <RouterLink to="/audit" active-class="current">Аудит</RouterLink>
      </template>
    </nav>
    <div class="session-nav" v-if="session.account"><RouterLink to="/account">{{ session.account.username }}</RouterLink><button :disabled="busy" @click="logout">Выйти</button></div>
    <div class="session-nav" v-else><RouterLink to="/login">Войти</RouterLink><RouterLink to="/register">Регистрация</RouterLink></div>
  </header>
  <main id="content" tabindex="-1"><ErrorBox :error="error" /><RouterView :key="route.path" /></main>
</template>
