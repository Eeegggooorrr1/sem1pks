<script setup lang="ts">
import { onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { listings } from '../api/listings'
import { reference } from '../api/admin'
import { UiError } from '../api/errors'
import { canEdit, session } from '../session'
import { useResource } from '../composables/useResource'
import { useTask } from '../composables/useTask'
import type { ListingInput } from '../types'
import ErrorBox from '../components/ErrorBox.vue'
import ListingForm from '../components/ListingForm.vue'
const route = useRoute(), router = useRouter()
const id = typeof route.params.id === 'string' ? route.params.id : ''
const { data, error: loadError, loading, refresh } = useResource(async () => {
  const [categories, listing] = await Promise.all([reference.list(), id ? listings.get(id) : Promise.resolve(undefined)])
  if (listing && !canEdit(listing.ownerId)) throw new UiError('У вас нет прав на изменение этого объявления.')
  if (listing?.status === 'CLOSED' && session.account?.role !== 'ADMIN') throw new UiError('Закрытое объявление нельзя изменить.')
  return { categories, listing }
})
const { busy, error, run } = useTask()
onMounted(refresh)
async function save(input: ListingInput) {
  await run(async () => {
    const listing = data.value?.listing
    const result = listing ? await listings.update(listing.id, listing.version, input) : await listings.create(input)
    await router.push(`/listings/${result.id}`)
  })
}
</script>
<template>
  <div class="page-heading"><h1>{{ id ? 'Изменить объявление' : 'Новое объявление' }}</h1></div>
  <ErrorBox :error="loadError"><button @click="refresh">Повторить</button></ErrorBox><ErrorBox :error="error"><button v-if="(error as { status?: number })?.status === 409" @click="refresh">Загрузить актуальную запись</button></ErrorBox>
  <p v-if="loading" role="status">Загрузка...</p>
  <ListingForm v-else-if="data && !loadError" :key="data.listing?.version ?? 'new'" :initial="data.listing" :categories="data.categories" :busy="busy" :cancel-to="id ? `/listings/${id}` : '/'" @submit="save" />
</template>
