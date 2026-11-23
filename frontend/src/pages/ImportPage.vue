<script setup lang="ts">
import { ref } from 'vue'
import { listings } from '../api/listings'
import { saveFile } from '../api/http'
import { UiError } from '../api/errors'
import { useTask } from '../composables/useTask'
import type { ImportMode, ImportPreview, ImportResult } from '../types'
import ErrorBox from '../components/ErrorBox.vue'
import ConfirmButton from '../components/ConfirmButton.vue'
const mode = ref<ImportMode>('NEW_ONLY'), file = ref<File | null>(null), preview = ref<ImportPreview | null>(null), result = ref<ImportResult | null>(null)
const { busy, error, run } = useTask()
function invalidate() { preview.value = null; result.value = null; error.value = null }
function select(event: Event) { file.value = (event.target as HTMLInputElement).files?.[0] || null; invalidate() }
async function check() {
  await run(async () => {
    if (!file.value) throw new UiError('Выберите CSV-файл.')
    if (file.value.size > 1_048_576) throw new UiError('Размер файла должен быть не больше 1 МБ.')
    preview.value = null; result.value = null
    preview.value = await listings.check(file.value, mode.value)
  })
}
async function apply() {
  if (!file.value || !preview.value?.confirmation) return
  await run(async () => {
    result.value = await listings.apply(file.value!, mode.value, preview.value!.confirmation!)
    preview.value = null
  })
}
function template() { saveFile(new Blob(['\uFEFFcode;title;description;price;status;categoryCode;featured;tags\r\nBOOK-001;Книга;Описание;1500,00;ACTIVE;BOOKS;false;"книги;набор"\r\n'], { type: 'text/csv;charset=utf-8' }), 'example.csv') }
</script>
<template>
  <div class="page-heading"><h1>Импорт CSV</h1><button @click="template">Скачать пример</button></div>
  <ErrorBox :error="error" />
  <form class="stack-form narrow-form" @submit.prevent="check"><fieldset :disabled="busy"><label>Файл CSV<input type="file" accept=".csv,.txt,text/csv,text/plain" required @change="select" /></label><label>Режим<select v-model="mode" @change="invalidate"><option value="NEW_ONLY">Только новые записи</option><option value="UPSERT">Создать или обновить по коду</option></select></label><p class="muted">CSV или текст Юникод из Excel. До 1 МБ и 1000 записей.</p><button class="primary" :disabled="!file">{{ busy ? 'Подождите...' : 'Проверить файл' }}</button></fieldset></form>
  <section v-if="preview"><h2>Результат проверки</h2><p role="status">Корректных записей: {{ preview.validRows }}. С ошибками: {{ preview.invalidRows }}.</p>
    <div v-if="preview.errors.length" class="table-wrap"><table><thead><tr><th>Запись</th><th>Ошибка</th></tr></thead><tbody><tr v-for="row in preview.errors" :key="row.row"><td>{{ row.row }}</td><td>{{ row.userMessage || 'Проверьте значения в этой строке.' }}</td></tr></tbody></table></div>
    <p v-if="preview.invalidRows" class="muted">Исправьте ошибки и проверьте файл заново.</p>
    <template v-if="preview.confirmation"><p class="muted">Проверка действует один час.</p><ConfirmButton label="Применить импорт" message="Записи из проверенного файла будут добавлены или обновлены в выбранном режиме." :busy="busy" @confirm="apply" /></template>
  </section>
  <div v-if="result" class="operation-result" role="status"><h2>Импорт завершен</h2><p>Создано: {{ result.created }}. Обновлено: {{ result.updated }}.</p><RouterLink to="/">Перейти к объявлениям</RouterLink></div>
</template>
