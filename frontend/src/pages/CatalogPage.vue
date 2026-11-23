<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { listings } from '../api/listings'
import { reference } from '../api/admin'
import { saveFile } from '../api/http'
import { operationError } from '../api/errors'
import { session } from '../session'
import { categoryName, date, emptyFilters, money, priorities, statuses } from '../format'
import type { BulkAction, BulkResult, Filters } from '../types'
import { useResource } from '../composables/useResource'
import { useTask } from '../composables/useTask'
import ErrorBox from '../components/ErrorBox.vue'
import Pagination from '../components/Pagination.vue'
import ConfirmButton from '../components/ConfirmButton.vue'

const route = useRoute(), router = useRouter()
const isAdmin = computed(() => session.account?.role === 'ADMIN')
const filter = reactive(emptyFilters()), selected = ref<string[]>([])
const action = ref<BulkAction>('ARCHIVE'), bulkStatus = ref('ACTIVE'), tag = ref('')
const result = ref<(BulkResult & { listingCodes: Record<string, string> }) | null>(null)
function fromUrl(): Filters {
  const next = emptyFilters()
  const allowed: Record<string, string[]> = { status: ['', 'ACTIVE', 'SOLD', 'CLOSED'], featured: ['', 'true', 'false'], priority: ['', 'LOW', 'MEDIUM', 'HIGH'], archived: ['false', 'true', 'all'], priceOp: ['EQ', 'GT', 'GTE', 'LT', 'LTE'], sortBy: ['TITLE', 'PRICE', 'CREATED_AT', 'UPDATED_AT'], direction: ['ASC', 'DESC'] }
  for (const key of ['search', 'categoryId', 'status', 'featured', 'priority', 'tag', 'archived', 'price', 'priceOp', 'sortBy', 'direction'] as const) {
    const value = route.query[key]
    if (typeof value === 'string' && (!allowed[key] || allowed[key].includes(value))) next[key] = value
  }
  next.page = Math.min(1000000, Math.max(0, Math.trunc(Number(route.query.page) || 0)))
  next.size = [10, 20, 50, 100].includes(Number(route.query.size)) ? Number(route.query.size) : 20
  if (!isAdmin.value) next.archived = 'false'
  return next
}
const { data, loading, error: loadError, refresh } = useResource(async () => {
  const current = fromUrl()
  const [page, categories] = await Promise.all([listings.list(current), reference.list()])
  return { page, categories }
})
const { busy, error, run } = useTask()
watch([() => route.query, isAdmin], () => { Object.assign(filter, fromUrl()); selected.value = []; result.value = null; void refresh() }, { immediate: true })
const editable = computed(() => data.value?.page.items || [])
async function navigate(page = 0, size = filter.size) {
  await router.push({ path: '/', query: Object.fromEntries(Object.entries({ ...filter, page, size }).filter(([, value]) => value !== '').map(([key, value]) => [key, String(value)])) })
}
async function reset() { Object.assign(filter, emptyFilters()); await router.push('/') }
async function applyBulk() {
  await run(async () => {
    const rows = editable.value.filter(row => selected.value.includes(row.id))
    const items = rows.map(({ id, version }) => ({ id, version }))
    const outcome = await listings.bulk({ items, action: action.value, ...(action.value === 'STATUS' ? { status: bulkStatus.value } : {}), ...(['ADD_TAG', 'REMOVE_TAG'].includes(action.value) ? { tag: tag.value } : {}) })
    result.value = { ...outcome, listingCodes: Object.fromEntries(rows.map(row => [row.id, row.code])) }
    selected.value = []; await refresh()
  })
}
async function exportCsv() { await run(async () => saveFile(await listings.export(fromUrl()), 'listings.csv')) }
const bulkReady = computed(() => selected.value.length > 0 && (!['ADD_TAG', 'REMOVE_TAG'].includes(action.value) || /^[\p{L}\p{N}_-]{1,40}$/u.test(tag.value)))
</script>
<template>
  <div class="page-heading"><h1>Объявления</h1><div class="actions"><button :disabled="busy || loading" @click="exportCsv">Скачать CSV</button><RouterLink v-if="isAdmin" class="button" to="/import">Импорт CSV</RouterLink><RouterLink v-if="session.account" class="button primary" to="/listings/new">Создать объявление</RouterLink></div></div>
  <form class="filters" @submit.prevent="navigate()">
    <fieldset :disabled="loading || busy">
      <div class="filter-grid">
        <label class="search-field">Поиск<input v-model="filter.search" maxlength="200" type="search" placeholder="Название, описание или код" /></label>
        <label>Категория<select v-model="filter.categoryId"><option value="">Все категории</option><option v-for="category in data?.categories" :key="category.id" :value="category.id">{{ categoryName(category.name) }}</option></select></label>
        <label>Статус<select v-model="filter.status"><option value="">Любой</option><option v-for="(label, value) in statuses" :key="value" :value="value">{{ label }}</option></select></label>
        <label v-if="isAdmin">Архив<select v-model="filter.archived"><option value="false">Вне архива</option><option value="true">В архиве</option><option value="all">Все записи</option></select></label>
      </div>
      <details class="extra-filters"><summary>Дополнительные фильтры</summary><div class="filter-grid">
        <label>Выделение<select v-model="filter.featured"><option value="">Любое</option><option value="true">Да</option><option value="false">Нет</option></select></label>
        <label>Приоритет<select v-model="filter.priority"><option value="">Любой</option><option v-for="(label, value) in priorities" :key="value" :value="value">{{ label }}</option></select></label>
        <label>Тег<input v-model="filter.tag" maxlength="40" /></label>
        <label>Сравнение цены<select v-model="filter.priceOp"><option value="EQ">Равна</option><option value="GT">Больше</option><option value="GTE">Не меньше</option><option value="LT">Меньше</option><option value="LTE">Не больше</option></select></label>
        <label>Цена<input v-model="filter.price" type="number" min="0" max="99999999.99" step="0.01" /></label>
      </div></details>
      <div class="filter-bottom"><div class="actions"><button class="primary" type="submit">Найти</button><button type="button" @click="reset">Сбросить</button></div><div class="actions">
        <label class="inline-label">Сортировка<select v-model="filter.sortBy"><option value="CREATED_AT">По дате создания</option><option value="UPDATED_AT">По дате изменения</option><option value="TITLE">По названию</option><option value="PRICE">По цене</option></select></label>
        <label class="sr-only" for="direction">Направление сортировки</label><select id="direction" v-model="filter.direction"><option value="DESC">По убыванию</option><option value="ASC">По возрастанию</option></select>
      </div></div>
    </fieldset>
  </form>
  <ErrorBox :error="loadError"><button @click="refresh">Повторить</button></ErrorBox><ErrorBox :error="error" />
  <div v-if="result" class="operation-result" role="status"><p>Успешно: {{ result.successful.length }}. Ошибок: {{ result.errors.length }}.</p><ul v-if="result.errors.length"><li v-for="item in result.errors" :key="item.id">{{ result.listingCodes[item.id] ?? 'Объявление' }}: {{ operationError(item.code, item.userMessage) }}</li></ul><button @click="result = null">Закрыть результат</button></div>
  <div v-if="isAdmin" class="bulk-bar">
    <span>Выбрано: {{ selected.length }}</span><label class="sr-only" for="bulk-action">Массовая операция</label><select id="bulk-action" v-model="action" :disabled="busy"><option value="ARCHIVE">Архивировать</option><option value="RESTORE">Восстановить</option><option value="STATUS">Изменить статус</option><option value="ADD_TAG">Добавить тег</option><option value="REMOVE_TAG">Удалить тег</option></select>
    <label v-if="action === 'STATUS'" class="inline-label">Статус<select v-model="bulkStatus"><option v-for="(label, value) in statuses" :key="value" :value="value">{{ label }}</option></select></label>
    <label v-if="['ADD_TAG', 'REMOVE_TAG'].includes(action)" class="inline-label">Тег<input v-model="tag" maxlength="40" /></label>
    <ConfirmButton label="Применить" :message="`Изменить выбранные объявления (${selected.length})?`" :busy="busy || loading || !bulkReady" @confirm="applyBulk" />
  </div>
  <div class="table-wrap catalog-table" :aria-busy="loading"><table><caption class="sr-only">Список объявлений</caption><thead><tr>
    <th v-if="isAdmin" class="check-cell"><input type="checkbox" aria-label="Выбрать все объявления на странице" :disabled="busy || loading || !editable.length" :checked="editable.length > 0 && selected.length === editable.length" @change="selected = ($event.target as HTMLInputElement).checked ? editable.map(row => row.id) : []" /></th>
    <th>Код</th><th>Название</th><th class="numeric">Цена</th><th>Категория</th><th>Статус</th><th>Приоритет</th><th>Теги</th><th>Создано</th><th v-if="isAdmin">Архив</th>
  </tr></thead><tbody>
    <tr v-if="loading && !data"><td :colspan="isAdmin ? 10 : 8" class="empty">Загрузка объявлений...</td></tr>
    <tr v-else-if="!data?.page.items.length"><td :colspan="isAdmin ? 10 : 8" class="empty">Объявления не найдены. Измените фильтры или сбросьте поиск.</td></tr>
    <tr v-for="row in data?.page.items" :key="row.id"><td v-if="isAdmin" class="check-cell"><input v-model="selected" type="checkbox" :value="row.id" :disabled="busy || loading" :aria-label="`Выбрать ${row.code}`" /></td><td class="code">{{ row.code }}</td><td class="title-cell"><RouterLink :to="`/listings/${row.id}`">{{ row.title }}</RouterLink><span v-if="row.featured" class="muted small">Выделено</span></td><td class="numeric">{{ money(row.price) }}</td><td>{{ categoryName(row.categoryName) }}</td><td>{{ statuses[row.status] }}</td><td>{{ priorities[row.priority] }}</td><td>{{ row.tags.join(', ') || '-' }}</td><td class="nowrap">{{ date(row.createdAt) }}</td><td v-if="isAdmin">{{ row.archived ? 'Да' : 'Нет' }}</td></tr>
  </tbody></table></div>
  <p v-if="loading && data" class="muted" role="status">Обновление списка...</p>
  <Pagination v-if="data" :page="data.page.page" :size="data.page.size" :total="data.page.total" :busy="loading || busy" @page="navigate($event)" @size="navigate(0, $event)" />
</template>
