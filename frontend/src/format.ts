import type { Filters } from './types'
export const statuses = { ACTIVE: 'Активно', SOLD: 'Продано', CLOSED: 'Закрыто' }
export const priorities = { LOW: 'Низкий', MEDIUM: 'Средний', HIGH: 'Высокий' }
const categoryNames: Record<string, string> = {
  ELECTRONICS: 'Электроника', REAL_ESTATE: 'Недвижимость', TRANSPORT: 'Транспорт', FURNITURE: 'Мебель', CLOTHING: 'Одежда',
  SPORTS: 'Спорт', BOOKS: 'Книги', BEAUTY: 'Красота', SERVICES: 'Услуги', JOBS: 'Работа', PETS: 'Животные', GARDEN: 'Сад', CONSTRUCTION: 'Строительство', CHILDREN: 'Детские товары', OTHER: 'Прочее',
}
export const categoryName = (name: string) => categoryNames[name] || name
export const date = (value: string) => new Intl.DateTimeFormat('ru-RU', { dateStyle: 'short', timeStyle: 'short' }).format(new Date(value))
export const money = (value: number) => new Intl.NumberFormat('ru-RU', { minimumFractionDigits: 2, maximumFractionDigits: 2 }).format(value)
export const emptyFilters = (): Filters => ({ search: '', categoryId: '', status: '', featured: '', priority: '', tag: '', archived: 'false', price: '', priceOp: 'EQ', sortBy: 'CREATED_AT', direction: 'DESC', page: 0, size: 20 })
