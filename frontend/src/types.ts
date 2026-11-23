export type Role = 'USER' | 'ADMIN'
export type Status = 'ACTIVE' | 'SOLD' | 'CLOSED'
export type Priority = 'LOW' | 'MEDIUM' | 'HIGH'
export interface Account { id: string; email: string; username: string; role: Role }
export interface AuthResponse { accessToken: string; refreshToken: string; account: Account }
export interface Page<T> { items: T[]; total: number; page: number; size: number }
export interface Category { id: string; code: string; name: string; enabled: boolean }
export interface CategoryInput { code: string; name: string; enabled: boolean }
export interface ListingInput {
  code: string; title: string; description: string | null; price: number; status: Status
  categoryId: string; featured: boolean; tags: string[]
}
export interface Listing extends ListingInput {
  id: string; categoryName: string; priority: Priority; archived: boolean; ownerId: string
  createdAt: string; updatedAt: string; version: number
}
export interface Filters {
  search: string; categoryId: string; status: string; featured: string; priority: string; tag: string
  archived: string; price: string; priceOp: string; sortBy: string; direction: string; page: number; size: number
}
export interface Note { id: string; authorId: string; authorName: string; body: string; pinned: boolean; createdAt: string; updatedAt: string }
export interface NoteInput { body: string; pinned: boolean }
export type BulkAction = 'ARCHIVE' | 'RESTORE' | 'STATUS' | 'ADD_TAG' | 'REMOVE_TAG'
export interface BulkInput { items: { id: string; version: number }[]; action: BulkAction; status?: string; tag?: string }
export interface BulkResult { successful: string[]; errors: { id: string; code: string; userMessage?: string }[] }
export type ImportMode = 'NEW_ONLY' | 'UPSERT'
export interface ImportPreview { confirmation: string | null; validRows: number; invalidRows: number; errors: { row: number; userMessage?: string }[] }
export interface ImportResult { created: number; updated: number }
export interface Group { key: string; count: number }
export interface Analytics { total: number; active: number; archived: number; featured: number; averagePrice: number; byCategory: Group[]; byStatus: Group[]; byPriority: Group[] }
export interface AdminUser { id: string; username: string; email: string; role: Role; active: boolean }
export type UserInput = Omit<AdminUser, 'id'>
export interface AuditEvent { id: string; userId: string | null; eventType: string; entityType: string; entityId: string | null; createdAt: string; description: string; correlationId: string | null }
export type OrderDirection = 'INCOMING' | 'OUTGOING'
export type OrderAction = 'confirm' | 'complete' | 'cancel'
export type OrderStatus = 'PENDING' | 'CONFIRMED' | 'COMPLETED' | 'CANCELLED'
export interface Order {
  id: string; listingId: string; listingTitle: string; buyerId: string; sellerId: string
  status: OrderStatus; orderDate: string
}
