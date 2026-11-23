import { clearSession, session, setSession } from '../session'
import type { AuthResponse } from '../types'
import { ApiError, UiError, responseError } from './errors'
let refreshing: Promise<void> | null = null
export function refreshSession(): Promise<void> {
  if (refreshing) return refreshing
  refreshing = (async () => {
    if (!session.refreshToken) throw new ApiError(401, 'Войдите в аккаунт.')
    let response: Response
    try {
      response = await fetch('/api/users/refresh', {
        method: 'POST', headers: { 'Content-Type': 'application/json' }, signal: AbortSignal.timeout(15000),
        body: JSON.stringify({ refreshToken: session.refreshToken }),
      })
    } catch { throw new UiError('Не удалось обновить сессию. Проверьте соединение и попробуйте снова.') }
    if (!response.ok) {
      if (response.status === 401 || response.status === 403) clearSession()
      throw await responseError(response, '/users/refresh')
    }
    setSession(await response.json() as AuthResponse)
  })().finally(() => { refreshing = null })
  return refreshing
}
export async function request<T>(path: string, init: RequestInit = {}, anonymous = false, retry = true): Promise<T> {
  const headers = new Headers(init.headers)
  if (init.body && !(init.body instanceof FormData)) headers.set('Content-Type', 'application/json')
  const token = anonymous ? '' : session.accessToken
  if (token) headers.set('Authorization', `Bearer ${token}`)
  let response: Response
  try { response = await fetch(`/api${path}`, { ...init, headers, signal: init.signal ?? AbortSignal.timeout(15000) }) }
  catch { throw new UiError('Нет соединения с сервером. Проверьте подключение и попробуйте снова.') }
  if (!response.ok) {
    const error = await responseError(response, path)
    if (error.status === 401 && error.code !== 'INVALID_CREDENTIALS' && !anonymous && retry && session.refreshToken) {
      if (session.accessToken === token) await refreshSession()
      return request<T>(path, init, false, false)
    }
    throw error
  }
  if (response.status === 204) return undefined as T
  if (response.headers.get('content-type')?.includes('json')) return response.json() as Promise<T>
  return response.blob() as Promise<T>
}
export function query(values: object): string {
  const params = new URLSearchParams()
  Object.entries(values).forEach(([key, value]) => {
    if (value !== '' && value !== null && value !== undefined) params.set(key, String(value))
  })
  return params.toString() ? `?${params}` : ''
}
export function saveFile(blob: Blob, name: string) {
  const url = URL.createObjectURL(blob)
  const anchor = document.createElement('a')
  anchor.href = url; anchor.download = name
  document.body.append(anchor); anchor.click(); anchor.remove()
  setTimeout(() => URL.revokeObjectURL(url), 1000)
}
