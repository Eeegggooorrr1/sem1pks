<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { orders } from '../api/orders'
import { date } from '../format'
import { useResource } from '../composables/useResource'
import { useTask } from '../composables/useTask'
import type { Order, OrderAction, OrderDirection } from '../types'
import ErrorBox from '../components/ErrorBox.vue'
import ConfirmButton from '../components/ConfirmButton.vue'

const props = defineProps<{ direction: OrderDirection }>()
const labels = { PENDING: 'Ожидает подтверждения', CONFIRMED: 'Подтвержден', COMPLETED: 'Завершен', CANCELLED: 'Отменен' }
const { data, error: loadError, loading, refresh } = useResource(() => orders.list(props.direction))
const { busy, error, run } = useTask()
const notice = ref('')
onMounted(refresh)

async function change(order: Order, action: OrderAction) {
  notice.value = ''
  await run(async () => {
    await orders.change(order.id, action)
    notice.value = { confirm: 'Заказ подтвержден.', complete: 'Покупка завершена.', cancel: 'Заказ отменен.' }[action]
    await refresh()
  })
}
</script>

<template>
  <div class="page-heading">
    <h1>{{ direction === 'OUTGOING' ? 'Покупки' : 'Продажи' }}</h1>
    <button :disabled="loading || busy" @click="refresh">Обновить</button>
  </div>
  <p v-if="notice" class="success" role="status">{{ notice }}</p>
  <ErrorBox :error="error"><button @click="refresh">Обновить список</button></ErrorBox>
  <ErrorBox :error="loadError"><button @click="refresh">Повторить</button></ErrorBox>
  <div class="table-wrap order-table" :aria-busy="loading">
    <table>
      <caption class="sr-only">{{ direction === 'OUTGOING' ? 'Исходящие заказы' : 'Входящие заказы' }}</caption>
      <thead><tr><th>Заказ</th><th>Объявление</th><th>Дата</th><th>Статус</th><th>Действия</th></tr></thead>
      <tbody>
        <tr v-if="!data?.length"><td colspan="5" class="empty">{{ loading ? 'Загрузка заказов...' : loadError ? 'Список недоступен.' : 'Заказов пока нет.' }}</td></tr>
        <tr v-for="order in data" :key="order.id">
          <td class="order-number">{{ order.id }}</td>
          <td><RouterLink :to="`/listings/${order.listingId}`">{{ order.listingTitle }}</RouterLink></td>
          <td class="nowrap">{{ date(order.orderDate) }}</td>
          <td>{{ labels[order.status] }}</td>
          <td><div class="actions">
            <button v-if="direction === 'INCOMING' && order.status === 'PENDING'" :disabled="busy || loading" @click="change(order, 'confirm')">Подтвердить</button>
            <button v-if="direction === 'OUTGOING' && order.status === 'CONFIRMED'" :disabled="busy || loading" @click="change(order, 'complete')">Завершить покупку</button>
            <ConfirmButton v-if="order.status === 'PENDING' || order.status === 'CONFIRMED'" label="Отменить заказ"
              :message="`Отменить заказ №${order.id}?`" :busy="busy || loading" @confirm="change(order, 'cancel')" />
          </div></td>
        </tr>
      </tbody>
    </table>
  </div>
</template>
