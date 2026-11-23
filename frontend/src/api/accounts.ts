import { request } from './http'
import { clearSession, session, setSession } from '../session'
import type { Account, AuthResponse } from '../types'
export const accounts = {
  async login(email: string, password: string) {
    setSession(await request<AuthResponse>('/users/login', { method: 'POST', body: JSON.stringify({ email, password }) }, true))
  },
  async register(username: string, email: string, password: string) {
    setSession(await request<AuthResponse>('/users/register', { method: 'POST', body: JSON.stringify({ username, email, password }) }, true))
  },
  current: () => request<Account>('/users/me'),
  async logout() {
    if (session.refreshToken) await request('/users/logout', { method: 'POST', body: JSON.stringify({ refreshToken: session.refreshToken }) }, true)
    clearSession()
  },
  async password(currentPassword: string, newPassword: string) {
    await request('/users/password', { method: 'POST', body: JSON.stringify({ currentPassword, newPassword }) })
    clearSession()
  },
}
