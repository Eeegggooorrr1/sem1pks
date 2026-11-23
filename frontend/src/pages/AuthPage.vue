<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { accounts } from '../api/accounts'
import { useTask } from '../composables/useTask'
import ErrorBox from '../components/ErrorBox.vue'
const route = useRoute(), router = useRouter()
const register = computed(() => route.path === '/register')
const username = ref(''), email = ref(''), password = ref(''), repeat = ref(''), mismatch = ref('')
const { busy, error, run } = useTask()
async function submit() {
  mismatch.value = ''
  if (register.value && password.value !== repeat.value) { mismatch.value = 'Пароли не совпадают.'; return }
  if (new TextEncoder().encode(password.value).length > 72) { mismatch.value = 'Пароль должен занимать не больше 72 байт UTF-8.'; return }
  await run(async () => {
    if (register.value) await accounts.register(username.value, email.value, password.value)
    else await accounts.login(email.value, password.value)
    const next = route.query.next
    await router.replace(typeof next === 'string' && next.startsWith('/') && !next.startsWith('//') ? next : '/')
  })
  password.value = ''; repeat.value = ''
}
</script>
<template>
  <div class="narrow-page"><h1>{{ register ? 'Регистрация' : 'Вход' }}</h1>
    <p v-if="route.query.changed" class="success" role="status">Пароль изменен. Войдите с новым паролем.</p>
    <ErrorBox :error="error" /><p v-if="mismatch" role="alert" class="field-error">{{ mismatch }}</p>
    <form class="stack-form" @submit.prevent="submit"><fieldset :disabled="busy">
      <label v-if="register">Имя<input v-model="username" required minlength="3" maxlength="20" autocomplete="nickname" /></label>
      <label>Email<input v-model="email" type="email" required maxlength="254" autocomplete="username" /></label>
      <label>Пароль<input v-model="password" type="password" required :minlength="register ? 8 : undefined" maxlength="72" :autocomplete="register ? 'new-password' : 'current-password'" /></label>
      <label v-if="register">Повторите пароль<input v-model="repeat" type="password" required minlength="8" maxlength="72" autocomplete="new-password" /></label>
      <button type="submit" class="primary">{{ busy ? 'Подождите...' : register ? 'Зарегистрироваться' : 'Войти' }}</button>
    </fieldset></form>
    <p><RouterLink :to="{ path: register ? '/login' : '/register', query: route.query.next ? { next: route.query.next } : {} }">{{ register ? 'Уже есть аккаунт? Войти' : 'Создать аккаунт' }}</RouterLink></p>
  </div>
</template>
