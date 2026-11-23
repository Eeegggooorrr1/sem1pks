<script setup lang="ts">
import { computed, nextTick, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { admin } from '../api/admin'
import { UiError } from '../api/errors'
import { clearSession, session } from '../session'
import type { AdminUser, UserInput } from '../types'
import { useResource } from '../composables/useResource'
import { useTask } from '../composables/useTask'
import ErrorBox from '../components/ErrorBox.vue'
import Pagination from '../components/Pagination.vue'
const router = useRouter(), page = ref(0), size = ref(20)
const { data, loading, error: loadError, refresh } = useResource(() => admin.users(page.value, size.value))
const { busy, error, run } = useTask()
const editing = ref<string | null>(null), open = ref(false), password = ref(''), notice = ref('')
const editingSelf = computed(() => editing.value === session.account?.id)
const form = reactive<UserInput>({ username: '', email: '', role: 'USER', active: true })
const editor = ref<HTMLFormElement>()
onMounted(refresh)
function edit(user?: AdminUser) {
  error.value = null; editing.value = user?.id ?? null
  Object.assign(form, user ? { username: user.username, email: user.email, role: user.role, active: user.active } : { username: '', email: '', role: 'USER', active: true })
  password.value = ''; open.value = true
  void nextTick(() => editor.value?.querySelector('input')?.focus())
}
async function save() {
  await run(async () => {
    if (!editing.value && new TextEncoder().encode(password.value).length > 72) throw new UiError('Выберите более короткий пароль.')
    if (editing.value) await admin.updateUser(editing.value, form); else await admin.createUser(form, password.value)
    if (editingSelf.value) { clearSession(); await router.replace('/login'); return }
    password.value = ''; open.value = false; notice.value = 'Пользователь сохранен.'; await refresh()
  })
}
async function changePage(value: number) { page.value = value; await refresh() }
async function changeSize(value: number) { size.value = value; page.value = 0; await refresh() }
</script>
<template>
  <div class="page-heading"><h1>Пользователи</h1><button class="primary" :disabled="busy" @click="edit()">Создать пользователя</button></div>
  <ErrorBox :error="loadError"><button @click="refresh">Повторить</button></ErrorBox><ErrorBox :error="error" /><p v-if="notice" role="status" class="success">{{ notice }}</p>
  <form v-if="open" ref="editor" class="inline-editor" @submit.prevent="save">
    <h2>{{ editing ? 'Изменить пользователя' : 'Новый пользователь' }}</h2>
    <fieldset :disabled="busy">
      <div class="form-grid">
        <label>Имя<input v-model="form.username" required minlength="3" maxlength="20" /></label>
        <label>Email<input v-model="form.email" required type="email" maxlength="254" autocomplete="off" /></label>
        <label>Роль
          <select v-model="form.role" :disabled="editingSelf">
            <option value="USER">Пользователь</option>
            <option value="ADMIN">Администратор</option>
          </select>
        </label>
        <label v-if="!editing">
          Пароль<input v-model="password" required type="password" minlength="8" maxlength="72" autocomplete="new-password" />
        </label>
        <label class="checkbox">
          <input v-model="form.active" type="checkbox" :disabled="editingSelf" />Аккаунт включен
        </label>
      </div>
      <p v-if="editingSelf" class="muted">После изменения своего аккаунта нужно войти заново.</p>
      <div class="actions">
        <button class="primary">{{ busy ? 'Сохранение...' : 'Сохранить' }}</button>
        <button type="button" @click="open = false; password = ''">Отмена</button>
      </div>
    </fieldset>
  </form>
  <p v-if="loading" role="status">Загрузка...</p><div class="table-wrap"><table><thead><tr><th>ID</th><th>Имя</th><th>Email</th><th>Роль</th><th>Состояние</th><th>Действия</th></tr></thead><tbody><tr v-if="!loading && !data?.items.length"><td colspan="6" class="empty">Пользователей не найдено.</td></tr><tr v-for="user in data?.items" :key="user.id"><td>{{ user.id }}</td><td>{{ user.username }}</td><td>{{ user.email }}</td><td>{{ user.role === 'ADMIN' ? 'Администратор' : 'Пользователь' }}</td><td>{{ user.active ? 'Включен' : 'Отключен' }}</td><td><button :disabled="busy" @click="edit(user)">Изменить</button></td></tr></tbody></table></div>
  <Pagination v-if="data" :page="data.page" :size="data.size" :total="data.total" :busy="loading || busy" @page="changePage" @size="changeSize" />
</template>
