import { request, query } from './http'
import type { Listing, ListingInput, Page, Filters, Note, NoteInput, BulkInput, BulkResult, ImportMode, ImportPreview, ImportResult } from '../types'
export function listingQuery(filter: Filters) {
  const values: Record<string, unknown> = { ...filter }
  if (!filter.price) { delete values.price; delete values.priceOp }
  if (filter.archived === 'all') delete values.archived
  const result = query(values)
  return filter.archived === 'all' ? `${result}${result ? '&' : '?'}archived=` : result
}
export const listings = {
  list: (filter: Filters) => request<Page<Listing>>(`/listings${listingQuery(filter)}`),
  get: (id: string) => request<Listing>(`/listings/${id}`),
  create: (data: ListingInput) => request<Listing>('/listings', { method: 'POST', body: JSON.stringify(data) }),
  update: (id: string, version: number, data: ListingInput) => request<Listing>(`/listings/${id}`, { method: 'PUT', body: JSON.stringify({ version, data }) }),
  archive: (row: Listing) => request<Listing>(`/listings/${row.id}?version=${row.version}`, { method: 'DELETE' }),
  close: (row: Listing) => request<Listing>(`/listings/${row.id}/close`, { method: 'POST', body: JSON.stringify({ version: row.version }) }),
  restore: (row: Listing) => request<Listing>(`/listings/${row.id}/restore`, { method: 'POST', body: JSON.stringify({ version: row.version }) }),
  delete: (row: Listing) => request<void>(`/listings/${row.id}/permanent?version=${row.version}`, { method: 'DELETE' }),
  bulk: (data: BulkInput) => request<BulkResult>('/listings/bulk', { method: 'POST', body: JSON.stringify(data) }),
  export: (filter: Filters) => request<Blob>(`/listings/export${listingQuery(filter)}`),
  check: (file: File, mode: ImportMode) => {
    const body = new FormData(); body.set('file', file); body.set('mode', mode)
    return request<ImportPreview>('/listings/import/check', { method: 'POST', body })
  },
  apply: (file: File, mode: ImportMode, confirmation: string) => {
    const body = new FormData()
    body.set('file', file)
    body.set('mode', mode)
    body.set('confirmation', confirmation)
    return request<ImportResult>('/listings/import/apply', { method: 'POST', body })
  },
  notes: (id: string) => request<Note[]>(`/listings/${id}/notes`),
  addNote: (id: string, data: NoteInput) => request<Note>(`/listings/${id}/notes`, { method: 'POST', body: JSON.stringify(data) }),
  updateNote: (id: string, noteId: string, data: NoteInput) => request<Note>(`/listings/${id}/notes/${noteId}`, { method: 'PUT', body: JSON.stringify(data) }),
  deleteNote: (id: string, noteId: string) => request<void>(`/listings/${id}/notes/${noteId}`, { method: 'DELETE' }),
}
