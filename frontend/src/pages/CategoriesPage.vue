<script setup lang="ts">
import { nextTick, onMounted, reactive, ref } from 'vue'
import { reference } from '../api/admin'
import type { Category } from '../types'
import { categoryName } from '../format'
import { useResource } from '../composables/useResource'
import { useTask } from '../composables/useTask'
import ErrorBox from '../components/ErrorBox.vue'
const { data, loading, error: loadError, refresh } = useResource(reference.list)
const { busy, error, run } = useTask()
const editing = ref<string | null>(null), open = ref(false), notice = ref('')
const form = reactive({ code: '', name: '', enabled: true })
const editor = ref<HTMLFormElement>()
onMounted(refresh)
function edit(category?: Category) {
  error.value = null; notice.value = ''; editing.value = category?.id || null
  Object.assign(form, category ? { code: category.code, name: category.name, enabled: category.enabled } : { code: '', name: '', enabled: true })
  open.value = true
  void nextTick(() => editor.value?.querySelector<HTMLInputElement>('input:not([readonly])')?.focus())
}
async function save() { await run(async () => { if (editing.value) await reference.update(editing.value, form); else await reference.create(form); open.value = false; notice.value = 'Категория сохранена.'; await refresh() }) }
</script>
<template>
  <div class="page-heading"><h1>Категории</h1><button class="primary" :disabled="busy" @click="edit()">Добавить категорию</button></div>
  <ErrorBox :error="loadError"><button @click="refresh">Повторить</button></ErrorBox><ErrorBox :error="error" /><p v-if="notice" class="success" role="status">{{ notice }}</p>
  <form v-if="open" ref="editor" class="inline-editor" @submit.prevent="save">
    <h2>{{ editing ? 'Изменить категорию' : 'Новая категория' }}</h2>
    <fieldset :disabled="busy">
      <div class="form-grid">
        <div>
          <label>
            Код
            <input
              v-model="form.code"
              required
              maxlength="20"
              pattern="[A-Z][A-Z0-9_]*"
              placeholder="HOME_GOODS"
              autocapitalize="characters"
              :spellcheck="false"
              :readonly="!!editing"
              :aria-describedby="editing ? undefined : 'category-code-hint'"
            />
          </label>
          <span v-if="!editing" id="category-code-hint" class="muted small">
            Заглавные латинские буквы, цифры и подчеркивание. Первый символ - буква.
          </span>
        </div>
        <label>Название<input v-model="form.name" required maxlength="100" /></label>
        <label class="checkbox"><input v-model="form.enabled" type="checkbox" />Включена</label>
      </div>
      <div class="actions">
        <button class="primary">{{ busy ? 'Сохранение...' : 'Сохранить' }}</button>
        <button type="button" @click="open = false">Отмена</button>
      </div>
    </fieldset>
  </form>
  <p v-if="loading" role="status">Загрузка...</p><div class="table-wrap"><table><thead><tr><th>Код</th><th>Название</th><th>Состояние</th><th>Действия</th></tr></thead><tbody><tr v-if="!loading && !data?.length"><td colspan="4" class="empty">Категорий пока нет.</td></tr><tr v-for="category in data" :key="category.id"><td class="code">{{ category.code }}</td><td>{{ categoryName(category.name) }}</td><td>{{ category.enabled ? 'Включена' : 'Отключена' }}</td><td><button :disabled="busy" @click="edit(category)">Изменить</button></td></tr></tbody></table></div>
</template>
