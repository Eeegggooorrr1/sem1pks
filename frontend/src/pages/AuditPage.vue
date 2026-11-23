<script setup lang="ts">
import { onMounted, reactive } from 'vue'
import { admin } from '../api/admin'
import { date } from '../format'
import { useResource } from '../composables/useResource'
import ErrorBox from '../components/ErrorBox.vue'
import Pagination from '../components/Pagination.vue'
const filter = reactive({ search: '', sortBy: 'CREATED_AT', direction: 'DESC', page: 0, size: 20 })
const { data, loading, error, refresh } = useResource(() => admin.audit(filter))
onMounted(refresh)
async function search() { filter.page = 0; await refresh() }
async function page(value: number) { filter.page = value; await refresh() }
async function size(value: number) { filter.size = value; filter.page = 0; await refresh() }
</script>
<template>
  <h1>Аудит</h1><form class="filters" @submit.prevent="search"><fieldset :disabled="loading"><div class="filter-grid"><label class="search-field">Поиск<input v-model="filter.search" type="search" maxlength="200" placeholder="Описание или тип события" /></label><label>Сортировка<select v-model="filter.sortBy"><option value="CREATED_AT">По времени</option><option value="EVENT_TYPE">По событию</option><option value="ENTITY_TYPE">По сущности</option></select></label><label>Порядок<select v-model="filter.direction"><option value="DESC">По убыванию</option><option value="ASC">По возрастанию</option></select></label></div><button class="primary" type="submit">Найти</button></fieldset></form>
  <ErrorBox :error="error"><button @click="refresh">Повторить</button></ErrorBox><p v-if="loading" role="status">Загрузка...</p>
  <div class="table-wrap"><table><thead><tr><th>Время</th><th>Пользователь (ID)</th><th>Событие</th><th>Сущность</th><th>ID записи</th><th>Описание</th><th>ID запроса</th></tr></thead><tbody><tr v-if="!loading && !data?.items.length"><td colspan="7" class="empty">События не найдены.</td></tr><tr v-for="event in data?.items" :key="event.id"><td class="nowrap">{{ date(event.createdAt) }}</td><td>{{ event.userId ?? '-' }}</td><td>{{ event.eventType }}</td><td>{{ event.entityType }}</td><td class="identifier">{{ event.entityId || '-' }}</td><td>{{ event.description }}</td><td class="identifier">{{ event.correlationId || '-' }}</td></tr></tbody></table></div>
  <Pagination v-if="data" :page="data.page" :size="data.size" :total="data.total" :busy="loading" @page="page" @size="size" />
</template>
