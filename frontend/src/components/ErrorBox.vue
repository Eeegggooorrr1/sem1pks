<script setup lang="ts">
import { computed } from 'vue'
import { UiError, validationDetails } from '../api/errors'
const props = defineProps<{ error: unknown }>()
const message = computed(() => props.error instanceof UiError ? props.error.message : 'Не удалось выполнить действие. Попробуйте еще раз.')
const details = computed(() => validationDetails(props.error))
</script>
<template>
  <div v-if="error" class="error-box" role="alert">
    <p>{{ message }}</p>
    <ul v-if="details.length"><li v-for="detail in details" :key="detail">{{ detail }}</li></ul>
    <slot />
  </div>
</template>
