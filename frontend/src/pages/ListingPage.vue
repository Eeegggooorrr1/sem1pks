<script setup lang="ts">
import { nextTick, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { listings } from '../api/listings'
import { orders } from '../api/orders'
import { canEdit, session } from '../session'
import { categoryName, date, money, priorities, statuses } from '../format'
import { useResource } from '../composables/useResource'
import { useTask } from '../composables/useTask'
import type { Note } from '../types'
import ErrorBox from '../components/ErrorBox.vue'
import ConfirmButton from '../components/ConfirmButton.vue'
const route = useRoute(), router = useRouter(), id = String(route.params.id)
const { data, loading, error: loadError, refresh } = useResource(async () => {
  const [listing, notes] = await Promise.all([listings.get(id), listings.notes(id)])
  return { listing, notes }
})
const { busy, error, run } = useTask()
const note = reactive({ body: '', pinned: false }), editing = ref<string | null>(null), notice = ref('')
onMounted(refresh)
async function archive() { await run(async () => { const listing = data.value!.listing; await (listing.archived ? listings.restore(listing) : listings.archive(listing)); await refresh() }) }
async function close() { await run(async () => { await listings.close(data.value!.listing); await refresh() }) }
async function buy() { await run(async () => { await orders.create(id); await router.push('/orders/outgoing') }) }
async function remove() { await run(async () => { await listings.delete(data.value!.listing); await router.push('/') }) }
function editNote(value: Note) { editing.value = value.id; Object.assign(note, { body: value.body, pinned: value.pinned }); notice.value = ''; void nextTick(() => document.querySelector<HTMLTextAreaElement>('.note-form textarea')?.focus()) }
function cancelNote() { editing.value = null; Object.assign(note, { body: '', pinned: false }) }
async function saveNote() { await run(async () => { if (editing.value) await listings.updateNote(id, editing.value, note); else await listings.addNote(id, note); cancelNote(); notice.value = 'Заметка сохранена.'; await refresh() }) }
async function deleteNote(noteId: string) { await run(async () => { await listings.deleteNote(id, noteId); if (editing.value === noteId) cancelNote(); await refresh() }) }
</script>
<template>
  <RouterLink to="/">К списку объявлений</RouterLink>
  <ErrorBox :error="loadError"><button @click="refresh">Повторить</button></ErrorBox><ErrorBox :error="error"><button v-if="(error as { status?: number })?.status === 409" @click="refresh">Обновить запись</button></ErrorBox>
  <p v-if="loading && !data" role="status">Загрузка...</p>
  <template v-if="data">
    <div class="page-heading">
      <h1>{{ data.listing.title }}</h1>
      <div class="actions">
        <template v-if="data.listing.status === 'ACTIVE' && !data.listing.archived">
          <ConfirmButton v-if="session.account && session.account.id !== data.listing.ownerId" label="Оформить заказ"
            :message="`Отправить продавцу заказ на «${data.listing.title}»?`" :busy="busy || loading" @confirm="buy" />
          <RouterLink v-else-if="!session.account" class="button" :to="{ path: '/login', query: { next: route.fullPath } }">Войти для покупки</RouterLink>
        </template>
        <template v-if="canEdit(data.listing.ownerId)">
          <RouterLink v-if="data.listing.status !== 'CLOSED' || session.account?.role === 'ADMIN'" class="button" :to="`/listings/${id}/edit`">Изменить</RouterLink>
          <ConfirmButton v-if="data.listing.status !== 'CLOSED' && !data.listing.archived" label="Закрыть объявление"
            message="После закрытия покупатели не смогут оформлять новые заказы." :busy="busy || loading" @confirm="close" />
        </template>
        <template v-if="session.account?.role === 'ADMIN'">
          <ConfirmButton :label="data.listing.archived ? 'Восстановить' : 'В архив'" :message="data.listing.archived ? 'Объявление вернется в основной список.' : 'Объявление переместится в архив. Его можно будет восстановить.'" :busy="busy || loading" @confirm="archive" />
          <ConfirmButton label="Удалить навсегда" message="Будут удалены объявление, заметки и связанные заказы. Отменить удаление нельзя." :danger="true" :busy="busy || loading" @confirm="remove" />
        </template>
      </div>
    </div>
    <section class="listing-details" aria-label="Сведения об объявлении">
      <dl class="listing-summary">
        <div class="listing-price">
          <dt>Цена</dt>
          <dd>{{ money(data.listing.price) }}</dd>
        </div>
        <div>
          <dt>Категория</dt>
          <dd>{{ categoryName(data.listing.categoryName) }}</dd>
        </div>
        <div>
          <dt>Статус</dt>
          <dd :class="{ 'listing-status-active': data.listing.status === 'ACTIVE' && !data.listing.archived }">
            {{ statuses[data.listing.status] }}
          </dd>
        </div>
      </dl>
      <div class="listing-description">
        <h2>Описание</h2>
        <p class="description" :class="{ muted: !data.listing.description }">
          {{ data.listing.description || 'Описание не добавлено.' }}
        </p>
      </div>
      <dl class="listing-metadata">
        <div class="listing-tags">
          <dt>Теги</dt>
          <dd>{{ data.listing.tags.join(', ') || 'Нет' }}</dd>
        </div>
        <div><dt>Код</dt><dd>{{ data.listing.code }}</dd></div>
        <div><dt>Приоритет</dt><dd>{{ priorities[data.listing.priority] }}</dd></div>
        <div><dt>Выделено</dt><dd>{{ data.listing.featured ? 'Да' : 'Нет' }}</dd></div>
        <div><dt>Создано</dt><dd>{{ date(data.listing.createdAt) }}</dd></div>
        <div><dt>Изменено</dt><dd>{{ date(data.listing.updatedAt) }}</dd></div>
        <div v-if="session.account?.role === 'ADMIN'">
          <dt>Архив</dt><dd>{{ data.listing.archived ? 'Да' : 'Нет' }}</dd>
        </div>
      </dl>
    </section>
    <section><h2>Заметки</h2><p v-if="notice" class="success" role="status">{{ notice }}</p>
      <div class="table-wrap"><table><caption class="sr-only">Заметки объявления</caption><thead><tr><th>Автор</th><th>Текст</th><th>Создано / изменено</th><th>Закреплена</th><th>Действия</th></tr></thead><tbody><tr v-if="!data.notes.length"><td colspan="5" class="empty">Заметок пока нет.</td></tr><tr v-for="value in data.notes" :key="value.id"><td>{{ value.authorName }}</td><td class="note-body">{{ value.body }}</td><td>{{ date(value.createdAt) }}<span v-if="value.createdAt !== value.updatedAt" class="muted small">{{ date(value.updatedAt) }}</span></td><td>{{ value.pinned ? 'Да' : 'Нет' }}</td><td><div v-if="session.account?.id === value.authorId || session.account?.role === 'ADMIN'" class="actions"><button :disabled="busy" @click="editNote(value)">Изменить</button><ConfirmButton label="Удалить" message="Удалить эту заметку?" :busy="busy" :danger="true" @confirm="deleteNote(value.id)" /></div></td></tr></tbody></table></div>
      <form v-if="session.account" class="note-form" @submit.prevent="saveNote"><h3>{{ editing ? 'Изменить заметку' : 'Добавить заметку' }}</h3><label>Текст заметки<textarea v-model="note.body" required maxlength="2000" rows="3" :disabled="busy" /></label><label class="checkbox"><input v-model="note.pinned" type="checkbox" :disabled="busy" />Закрепить</label><div class="actions"><button class="primary" :disabled="busy || !note.body.trim()">{{ busy ? 'Сохранение...' : 'Сохранить заметку' }}</button><button v-if="editing" type="button" @click="cancelNote">Отмена</button></div></form>
    </section>
  </template>
</template>
