<script setup lang="ts">
import { reactive, ref } from 'vue'
import type { Category, ListingInput } from '../types'
import { categoryName, statuses } from '../format'
const props = defineProps<{ initial?: ListingInput; categories: Category[]; busy: boolean; cancelTo: string }>()
const emit = defineEmits<{ submit: [data: ListingInput] }>()
const form = reactive<ListingInput>(props.initial ? { ...props.initial, tags: [...props.initial.tags] } : { code: '', title: '', description: '', price: 1, status: 'ACTIVE', categoryId: '', featured: false, tags: [] })
const tags = ref(form.tags.join(', '))
const tagError = ref('')
function submit() {
  const values = [...new Set(tags.value.split(',').map(value => value.trim()).filter(Boolean))]
  if (values.length > 20 || values.some(value => !/^[\p{L}\p{N}_-]{1,40}$/u.test(value))) {
    tagError.value = 'Не больше 20 тегов. В теге допустимы буквы, цифры, дефис и подчеркивание, до 40 символов.'; return
  }
  tagError.value = ''
  emit('submit', { ...form, title: form.title.trim(), tags: values })
}
</script>
<template>
  <form class="editor-form" @submit.prevent="submit">
    <fieldset :disabled="busy">
      <div class="form-grid">
        <label>Код<input v-model="form.code" required maxlength="64" pattern="[A-Za-z0-9][A-Za-z0-9._\-]*" /></label>
        <label>Категория<select v-model="form.categoryId" required><option value="" disabled>Выберите категорию</option><option v-for="category in categories" :key="category.id" :value="category.id" :disabled="!category.enabled && category.id !== initial?.categoryId">{{ categoryName(category.name) }}{{ category.enabled ? '' : ' (отключена)' }}</option></select></label>
        <label class="wide">Название<input v-model="form.title" required maxlength="200" /></label>
        <label class="wide">Описание<textarea v-model="form.description" rows="5" maxlength="10000" /></label>
        <label>Цена<input v-model.number="form.price" type="number" required min="0.01" max="99999999.99" step="0.01" /></label>
        <label>Статус<select v-model="form.status"><option v-for="(label, value) in statuses" :key="value" :value="value">{{ label }}</option></select></label>
        <label class="wide">Теги через запятую<input v-model="tags" maxlength="850" :aria-invalid="!!tagError" aria-describedby="tag-error" /><span v-if="tagError" id="tag-error" class="field-error">{{ tagError }}</span></label>
        <label class="checkbox wide"><input v-model="form.featured" type="checkbox" />Выделенное объявление</label>
      </div>
    </fieldset>
    <div class="actions"><button class="primary" :disabled="busy" type="submit">{{ busy ? 'Сохранение...' : 'Сохранить' }}</button><RouterLink class="button" :to="cancelTo">Отмена</RouterLink></div>
  </form>
</template>
