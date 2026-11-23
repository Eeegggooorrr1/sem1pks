import { reactive } from 'vue'
import type { AuthResponse, Account } from './types'

const key = 'sem1pks.refresh'
function storedToken(): string | null {
  try { return sessionStorage.getItem(key) } catch { return null }
}
export const session = reactive({ account: null as Account | null, accessToken: '', refreshToken: storedToken() })
export function setSession(data: AuthResponse) {
  session.account = data.account
  session.accessToken = data.accessToken
  session.refreshToken = data.refreshToken
  try { sessionStorage.setItem(key, data.refreshToken) } catch {}
}
export function clearSession() {
  session.account = null
  session.accessToken = ''
  session.refreshToken = null
  try { sessionStorage.removeItem(key) } catch {}
}
export function canEdit(ownerId: string) { return session.account?.role === 'ADMIN' || session.account?.id === ownerId }
