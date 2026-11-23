import { request, query } from './http'
import type { Category, CategoryInput, Page, AdminUser, UserInput, AuditEvent, Analytics } from '../types'
export const reference = {
  list: () => request<Category[]>('/categories'),
  create: (data: CategoryInput) => request<Category>('/categories', { method: 'POST', body: JSON.stringify(data) }),
  update: (id: string, data: CategoryInput) => request<Category>(`/categories/${id}`, { method: 'PUT', body: JSON.stringify(data) }),
}
export const admin = {
  users: (page: number, size: number) => request<Page<AdminUser>>(`/admin/users${query({ page, size })}`),
  createUser: (user: UserInput, password: string) => request<AdminUser>('/admin/users', { method: 'POST', body: JSON.stringify({ user, password }) }),
  updateUser: (id: string, data: UserInput) => request<AdminUser>(`/admin/users/${id}`, { method: 'PUT', body: JSON.stringify(data) }),
  audit: (filter: { search: string; page: number; size: number; sortBy: string; direction: string }) => request<Page<AuditEvent>>(`/admin/audit${query(filter)}`),
  export: () => request<Blob>('/admin/export'),
}
export const getAnalytics = () => request<Analytics>('/analytics')
