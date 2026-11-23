export class UiError extends Error {}

export class ApiError extends UiError {
  constructor(public status: number, message: string, public code = '', public details: Record<string, string[]> = {}) {
    super(message)
  }
}

const messages: Readonly<Record<string, string>> = {
  ACCOUNT_BLOCKED: 'Аккаунт заблокирован. Если это ошибка, обратитесь к администратору.',
  ACCOUNT_ALREADY_EXISTS: 'Аккаунт с таким email уже есть. Войдите или укажите другой адрес.',
  ACCOUNT_NOT_FOUND: 'Пользователь не найден. Обновите список.',
  ADMIN_SELF_ACCESS_CHANGE: 'Свою роль и состояние аккаунта изменить нельзя.',
  NOT_FOUND: 'Запись не найдена. Возможно, ее удалили.',
  LISTING_NOT_FOUND: 'Объявление больше недоступно.',
  ORDER_NOT_FOUND_EXCEPTION: 'Заказ не найден. Обновите список.',
  LISTING_ACCESS_DENIED: 'У вас нет прав на изменение этого объявления.',
  ORDER_ACCESS_DENIED: 'У вас нет прав на изменение этого заказа.',
  FORBIDDEN: 'У вас нет прав на это действие.',
  LISTING_STATE_CONFLICT: 'Действие недоступно при текущем статусе объявления. Обновите страницу.',
  ORDER_STATE_CONFLICT: 'Действие недоступно при текущем статусе заказа. Обновите список.',
  CONCURRENT_UPDATE: 'Запись уже изменилась. Обновите ее и повторите действие.',
  DATA_CONFLICT: 'Такая запись уже есть или данные изменились. Проверьте введенные значения.',
  VALIDATION_ERROR: 'Проверьте указанные данные и попробуйте снова.',
  INVALID_FILE: 'Выберите CSV размером не больше 1 МБ.',
  MALFORMED_JSON: 'Не удалось прочитать введенные данные. Проверьте поля и попробуйте снова.',
  TYPE_MISMATCH: 'Некоторые значения имеют неверный формат. Проверьте их и попробуйте снова.',
  MISSING_PARAMETER: 'Не хватает данных для этого действия. Заполните обязательные поля.',
  AUTH_RATE_LIMIT: 'Слишком много запросов. Попробуйте снова через минуту.',
  INVALID_TOKEN: 'Сессия недействительна. Войдите снова.',
}

function fallback(status: number): string {
  switch (status) {
    case 400: return 'Проверьте введенные данные и попробуйте снова.'
    case 401: return 'Войдите в аккаунт, чтобы продолжить.'
    case 403: return 'У вас нет прав на это действие.'
    case 404: return 'Запись не найдена. Возможно, ее удалили.'
    case 405: return 'Это действие недоступно. Обновите страницу и попробуйте снова.'
    case 409: return 'Действие не удалось выполнить. Обновите данные и попробуйте снова.'
    case 413: return 'Файл слишком большой. Выберите файл размером не больше 1 МБ.'
    case 429: return 'Слишком много запросов. Подождите немного и попробуйте снова.'
    case 502:
    case 503:
    case 504: return 'Сервис временно недоступен. Попробуйте чуть позже.'
    default: return 'Не удалось выполнить действие. Попробуйте еще раз.'
  }
}

function publicMessage(value: unknown): string | undefined {
  return typeof value === 'string' && value.trim() ? value.trim() : undefined
}

export function operationError(code: string, userMessage?: string, status = 0): string {
  if (Object.hasOwn(messages, code)) return messages[code]!
  if (['INVALID_DATA', 'CONFLICT', 'ORDER_CREATE_CONFLICT_EXCEPTION'].includes(code)) {
    return publicMessage(userMessage) ?? fallback(status)
  }
  return fallback(status)
}

function errorText(code: string, status: number, path: string, userMessage?: string) {
  if (code === 'INVALID_CREDENTIALS') {
    if (path === '/users/password') return 'Текущий пароль указан неверно.'
    if (path === '/users/refresh') return 'Сессия завершена. Войдите снова.'
    return 'Неверный email или пароль. Проверьте их и попробуйте снова.'
  }
  if (status === 401 && path === '/users/refresh') return 'Сессия завершена. Войдите снова.'
  if (status >= 500) return fallback(status)
  return operationError(code, userMessage, status)
}

function fieldDetails(value: unknown): Record<string, string[]> {
  if (typeof value !== 'object' || value === null || Array.isArray(value)) return {}
  return Object.fromEntries(Object.entries(value).flatMap(([field, issues]) => {
    if (!Array.isArray(issues)) return []
    const descriptions = issues.map(publicMessage).filter((message): message is string => Boolean(message))
    return descriptions.length ? [[field, descriptions]] : []
  }))
}

export async function responseError(response: Response, path: string): Promise<ApiError> {
  const body: unknown = await response.json().catch(() => null)
  const data = typeof body === 'object' && body !== null ? body as Record<string, unknown> : {}
  const code = typeof data.errorCode === 'string' ? data.errorCode : ''
  const message = publicMessage(data.userMessage)
  const details = response.status === 400 && code === 'VALIDATION_ERROR' && message ? fieldDetails(data.details) : {}
  return new ApiError(response.status, errorText(code, response.status, path, message), code, details)
}

const fieldLabels: Readonly<Record<string, string>> = {
  title: 'Название', code: 'Код', name: 'Название', description: 'Описание', price: 'Цена',
  categoryId: 'Категория', status: 'Статус', tags: 'Теги', tag: 'Тег', featured: 'Выделение',
  username: 'Имя', email: 'Email', password: 'Пароль', currentPassword: 'Текущий пароль',
  newPassword: 'Новый пароль', role: 'Роль', active: 'Аккаунт включен', enabled: 'Категория включена',
  body: 'Текст заметки', pinned: 'Закрепление', items: 'Выбранные записи', file: 'Файл',
  search: 'Поиск', direction: 'Порядок', sortBy: 'Сортировка', size: 'Размер страницы',
}

export function validationDetails(error: unknown): string[] {
  if (!(error instanceof ApiError)) return []
  const lines = Object.entries(error.details).flatMap(([field, issues]) => {
    const name = field.split('.').map(part => part.split('[')[0]!).reverse()
      .find(part => Object.hasOwn(fieldLabels, part))
    return issues.map(issue => name ? `${fieldLabels[name]}: ${issue}` : issue)
  })
  return [...new Set(lines)]
}
