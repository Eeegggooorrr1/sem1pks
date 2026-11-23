<script setup lang="ts">
import { ref } from 'vue'
defineProps<{ label: string; message: string; busy?: boolean; danger?: boolean }>()
const emit = defineEmits<{ confirm: [] }>()
const dialog = ref<HTMLDialogElement>()
function confirm() { dialog.value?.close(); emit('confirm') }
</script>
<template>
  <button type="button" :class="{ danger }" :disabled="busy" @click="dialog?.showModal()">{{ label }}</button>
  <dialog ref="dialog" aria-label="Подтверждение действия">
    <h2>{{ label }}?</h2><p>{{ message }}</p>
    <div class="actions end"><button type="button" autofocus @click="dialog?.close()">Отмена</button><button type="button" :class="danger ? 'danger' : 'primary'" @click="confirm">{{ label }}</button></div>
  </dialog>
</template>
