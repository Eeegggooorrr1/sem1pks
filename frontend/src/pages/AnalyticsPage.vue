<script setup lang="ts">
import { computed, onMounted } from 'vue'
import { getAnalytics, reference } from '../api/admin'
import { useResource } from '../composables/useResource'
import { categoryName, money, priorities, statuses } from '../format'
import ErrorBox from '../components/ErrorBox.vue'
const { data, loading, error, refresh } = useResource(async () => {
  const [stats, categories] = await Promise.all([getAnalytics(), reference.list()])
  return { stats, categories }
})
onMounted(refresh)
function category(id: string) { return categoryName(data.value?.categories.find(value => value.id === id)?.name || id) }
const tables = computed(() => {
  const stats = data.value?.stats
  if (!stats) return []
  return [
    { title: 'Общие показатели', label: 'Показатель', value: 'Значение', rows: [
      { name: 'Всего объявлений', value: stats.total },
      { name: 'Вне архива', value: stats.active },
      { name: 'В архиве', value: stats.archived },
      { name: 'Выделенных', value: stats.featured },
      { name: 'Средняя цена', value: money(stats.averagePrice) },
    ] },
    { title: 'По статусам', label: 'Статус', value: 'Количество', rows: stats.byStatus.map(row => ({ name: statuses[row.key as keyof typeof statuses] || row.key, value: row.count })) },
    { title: 'По приоритету', label: 'Приоритет', value: 'Количество', rows: stats.byPriority.map(row => ({ name: priorities[row.key as keyof typeof priorities] || row.key, value: row.count })) },
    { title: 'По категориям', label: 'Категория', value: 'Количество', rows: stats.byCategory.map(row => ({ name: category(row.key), value: row.count })).sort((a, b) => b.value - a.value || a.name.localeCompare(b.name, 'ru')) },
  ]
})
</script>
<template>
  <div class="page-heading"><h1>Аналитика</h1><button :disabled="loading" @click="refresh">Обновить</button></div>
  <ErrorBox :error="error"><button @click="refresh">Повторить</button></ErrorBox><p v-if="loading" role="status">Загрузка...</p>
  <div v-if="data" class="analytics-tables">
    <div v-for="table in tables" :key="table.title" class="table-wrap">
      <table>
        <caption>{{ table.title }}</caption>
        <thead><tr><th scope="col">{{ table.label }}</th><th scope="col" class="numeric">{{ table.value }}</th></tr></thead>
        <tbody>
          <tr v-if="!table.rows.length"><td colspan="2" class="empty">Нет данных</td></tr>
          <tr v-for="(row, index) in table.rows" :key="index"><th scope="row">{{ row.name }}</th><td class="numeric">{{ row.value }}</td></tr>
        </tbody>
      </table>
    </div>
  </div>
</template>
