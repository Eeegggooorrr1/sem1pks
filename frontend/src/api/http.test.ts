import { beforeEach, describe, expect, it, vi } from 'vitest'
import { request, query } from './http'
import { ApiError, validationDetails } from './errors'
import { clearSession, session, setSession } from '../session'
import { listingQuery } from './listings'
import { emptyFilters } from '../format'

beforeEach(() => { vi.unstubAllGlobals(); clearSession() })
const login = () => setSession({ accessToken: 'old', refreshToken: 'refresh-old', account: { id: "00000000-0000-0000-0000-000000000001", username: 'User', email: 'user@example.com', role: 'USER' } })

describe('HTTP client', () => {
  it('refreshes once for simultaneous unauthorized requests and retries with the new token', async () => {
    login()
    let refreshes = 0
    const fetch = vi.fn(async (path: string, init: RequestInit) => {
      if (path === '/api/users/refresh') {
        refreshes++
        await new Promise(resolve => setTimeout(resolve, 10))
        return Response.json({ accessToken: 'new', refreshToken: 'refresh-new', account: session.account })
      }
      return new Headers(init.headers).get('Authorization') === 'Bearer new' ? Response.json({ ok: true }) : Response.json({ message: 'Expired' }, { status: 401 })
    })
    vi.stubGlobal('fetch', fetch)
    expect(await Promise.all([request('/first'), request('/second')])).toEqual([{ ok: true }, { ok: true }])
    expect(refreshes).toBe(1)
    expect(fetch).toHaveBeenCalledTimes(5)
    expect(session.refreshToken).toBe('refresh-new')
  })
  it('clears a revoked session without retrying indefinitely', async () => {
    login()
    const fetch = vi.fn(async () => Response.json({ message: 'Сессия отозвана' }, { status: 401 }))
    vi.stubGlobal('fetch', fetch)
    await expect(request('/private')).rejects.toMatchObject({ status: 401, message: 'Сессия завершена. Войдите снова.' })
    expect(session.account).toBeNull()
    expect(session.refreshToken).toBeNull()
    expect(fetch).toHaveBeenCalledTimes(2)
  })
  it('keeps the session on a permission error without showing the server message', async () => {
    login()
    vi.stubGlobal('fetch', vi.fn(async () => Response.json({ errorCode: 'FORBIDDEN', message: 'Access is denied', details: { title: ['Technical detail'] } }, { status: 403 })))
    await expect(request('/private')).rejects.toMatchObject({ status: 403, message: 'У вас нет прав на это действие.', details: {} })
    expect(session.accessToken).toBe('old')
  })
  it('uses Russian field labels for nested validation errors', async () => {
    vi.stubGlobal('fetch', vi.fn(async () => Response.json({
      errorCode: 'VALIDATION_ERROR', message: 'Validation failed', userMessage: 'Проверьте данные.',
      details: { 'user.email': ['Укажите значение.'], 'data.tags[].<iterable element>': ['Проверьте формат значения.'] },
    }, { status: 400 })))
    const error = await request('/private').catch(error => error)
    expect(validationDetails(error)).toEqual(['Email: Укажите значение.', 'Теги: Проверьте формат значения.'])
    expect(error).toMatchObject({ message: 'Проверьте указанные данные и попробуйте снова.' })
  })
  it('ignores unclassified messages and keeps intentional CSV guidance', async () => {
    vi.stubGlobal('fetch', vi.fn()
      .mockResolvedValueOnce(Response.json({ errorCode: 'VALIDATION_ERROR', message: 'Invalid field', details: { title: ['must not be blank'] } }, { status: 400 }))
      .mockResolvedValueOnce(Response.json({ errorCode: 'INVALID_DATA', message: 'Internal detail', userMessage: 'Проверьте заголовки столбцов.' }, { status: 400 }))
      .mockResolvedValueOnce(Response.json({ message: 'Класс SQLException, ошибка запроса' }, { status: 500 })))
    await expect(request('/listings')).rejects.toMatchObject({ details: {}, message: 'Проверьте указанные данные и попробуйте снова.' })
    await expect(request('/listings/import/check')).rejects.toThrow('Проверьте заголовки столбцов.')
    await expect(request('/listings')).rejects.toThrow('Не удалось выполнить действие. Попробуйте еще раз.')
  })
  it('shows a wrong current password without refreshing the session', async () => {
    login()
    const fetch = vi.fn(async () => Response.json({ errorCode: 'INVALID_CREDENTIALS', message: 'Bad credentials' }, { status: 401 }))
    vi.stubGlobal('fetch', fetch)
    await expect(request('/users/password', { method: 'POST' })).rejects.toThrow('Текущий пароль указан неверно.')
    expect(fetch).toHaveBeenCalledTimes(1)
    expect(session.refreshToken).toBe('refresh-old')
  })
  it('does not attach an old access token to sign-in requests', async () => {
    login()
    const fetch = vi.fn(async () => Response.json({ ok: true }))
    vi.stubGlobal('fetch', fetch)
    await request('/users/login', { method: 'POST', body: '{}' }, true)
    const options = fetch.mock.calls[0] as unknown as [string, RequestInit]
    expect(new Headers(options[1].headers).has('Authorization')).toBe(false)
  })
  it('leaves multipart content type to the browser and reads empty responses', async () => {
    const fetch = vi.fn(async () => new Response(null, { status: 204 }))
    vi.stubGlobal('fetch', fetch)
    const form = new FormData(); form.set('mode', 'NEW_ONLY')
    expect(await request('/upload', { method: 'POST', body: form })).toBeUndefined()
    const options = fetch.mock.calls[0] as unknown as [string, RequestInit]
    expect(new Headers(options[1].headers).has('Content-Type')).toBe(false)
  })
  it('returns a readable error for an unavailable server or non-JSON proxy error', async () => {
    vi.stubGlobal('fetch', vi.fn(async () => { throw new TypeError('Failed to fetch') }))
    await expect(request('/listings')).rejects.toThrow('Нет соединения с сервером')
    vi.stubGlobal('fetch', vi.fn(async () => new Response('<html>bad gateway</html>', { status: 502 })))
    await expect(request('/listings')).rejects.toBeInstanceOf(ApiError)
  })
})

describe('query parameters', () => {
  it('encodes free text and includes false and zero values', () => {
    const result = new URLSearchParams(query({ search: "' &?", page: 0, featured: false, absent: '' }).slice(1))
    expect(result.get('search')).toBe("' &?")
    expect(result.get('page')).toBe('0')
    expect(result.get('featured')).toBe('false')
    expect(result.has('absent')).toBe(false)
  })
  it('requests both archived and active rows explicitly and omits unused price comparison', () => {
    const filter = emptyFilters(); filter.archived = 'all'
    const result = new URLSearchParams(listingQuery(filter).slice(1))
    expect(result.has('archived')).toBe(true)
    expect(result.get('archived')).toBe('')
    expect(result.has('priceOp')).toBe(false)
    filter.price = '0'
    expect(new URLSearchParams(listingQuery(filter).slice(1)).get('price')).toBe('0')
  })
})
