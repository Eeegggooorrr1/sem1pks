<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { accounts } from '../api/accounts'
import { admin } from '../api/admin'
import { saveFile } from '../api/http'
import { useResource } from '../composables/useResource'
import { useTask } from '../composables/useTask'
import ErrorBox from '../components/ErrorBox.vue'
const router = useRouter()
const { data, loading, error: loadError, refresh } = useResource(accounts.current)
const { busy, error, run } = useTask()
const current = ref(''), password = ref(''), repeat = ref(''), mismatch = ref('')
onMounted(refresh)
async function changePassword() {
  mismatch.value = ''
  if (password.value !== repeat.value) { mismatch.value = 'Пароли не совпадают.'; return }
  if (new TextEncoder().encode(password.value).length > 72) { mismatch.value = 'Пароль должен занимать не больше 72 байт UTF-8.'; return }
  await run(async () => { await accounts.password(current.value, password.value); await router.replace('/login?changed=true') })
  current.value = ''; password.value = ''; repeat.value = ''
}
async function exportAll() { await run(async () => saveFile(await admin.export(), 'domain-export.zip')) }
</script>
<template>
  <h1>Аккаунт</h1><ErrorBox :error="loadError"><button @click="refresh">Повторить</button></ErrorBox><p v-if="loading" role="status">Загрузка...</p>
  <dl v-if="data" class="record-fields"><dt>Имя</dt><dd>{{ data.username }}</dd><dt>Email</dt><dd>{{ data.email }}</dd><dt>Роль</dt><dd>{{ data.role === 'ADMIN' ? 'Администратор' : 'Пользователь' }}</dd></dl>
  <h2>Смена пароля</h2><ErrorBox :error="error" /><p v-if="mismatch" role="alert" class="field-error">{{ mismatch }}</p>
  <form class="stack-form narrow-form" @submit.prevent="changePassword"><fieldset :disabled="busy"><label>Текущий пароль<input v-model="current" type="password" required maxlength="72" autocomplete="current-password" /></label><label>Новый пароль<input v-model="password" type="password" required minlength="8" maxlength="72" autocomplete="new-password" /></label><label>Повторите новый пароль<input v-model="repeat" type="password" required minlength="8" maxlength="72" autocomplete="new-password" /></label><button class="primary">{{ busy ? 'Сохранение...' : 'Сменить пароль' }}</button></fieldset></form>
  <section v-if="data?.role === 'ADMIN'"><h2>Экспорт базы</h2><button :disabled="busy" @click="exportAll">Скачать ZIP с CSV</button></section>
</template>
