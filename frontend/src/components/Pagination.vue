<script setup lang="ts">
defineProps<{ page: number; size: number; total: number; busy?: boolean }>()
defineEmits<{ page: [value: number]; size: [value: number] }>()
</script>
<template>
  <div class="pagination" aria-label="Страницы">
    <span>{{ total ? page * size + 1 : 0 }}-{{ Math.min((page + 1) * size, total) }} из {{ total }}</span>
    <div class="actions">
      <label class="inline-label">На странице
        <select :value="size" :disabled="busy" @change="$emit('size', Number(($event.target as HTMLSelectElement).value))">
          <option :value="10">10</option><option :value="20">20</option><option :value="50">50</option><option :value="100">100</option>
        </select>
      </label>
      <button :disabled="busy || page === 0" @click="$emit('page', page - 1)">Назад</button>
      <button :disabled="busy || (page + 1) * size >= total" @click="$emit('page', page + 1)">Далее</button>
    </div>
  </div>
</template>
