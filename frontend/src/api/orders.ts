import { request } from './http'
import type { Order, OrderAction, OrderDirection } from '../types'

export const orders = {
  list: (direction: OrderDirection) => request<Order[]>(`/orders?direction=${direction}`),
  create: (listingId: string) => request<Order>(`/orders/listings/${listingId}`, { method: 'POST' }),
  change: (id: string, action: OrderAction) => request<Order | undefined>(`/orders/${id}/${action}`, { method: 'PATCH' }),
}
