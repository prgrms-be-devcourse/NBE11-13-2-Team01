import type { ErrorResponse, TokenResponse } from '../types/api'

const ACCESS_TOKEN_KEY = 'delivery-insight.access-token'
const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? ''
export const AUTH_UNAUTHORIZED_EVENT = 'delivery-insight:unauthorized'

export class ApiError extends Error {
  status: number
  response: ErrorResponse | null

  constructor(status: number, response: ErrorResponse | null) {
    super(response?.reason || response?.message || '요청을 처리하지 못했습니다.')
    this.name = 'ApiError'
    this.status = status
    this.response = response
  }
}

export function getAccessToken() {
  return localStorage.getItem(ACCESS_TOKEN_KEY)
}

export function setAccessToken(token: string) {
  localStorage.setItem(ACCESS_TOKEN_KEY, token)
}

export function clearAccessToken() {
  localStorage.removeItem(ACCESS_TOKEN_KEY)
}

function apiUrl(path: string) {
  return `${API_BASE_URL}${path}`
}

async function readJson<T>(response: Response): Promise<T | null> {
  const text = await response.text()
  if (!text) return null

  try {
    return JSON.parse(text) as T
  } catch {
    return null
  }
}

async function refreshAccessToken() {
  const response = await fetch(apiUrl('/api/tokens/refresh'), {
    method: 'POST',
    credentials: 'include',
  })

  if (!response.ok) return null

  const body = await readJson<TokenResponse>(response)
  if (!body?.accessToken) return null

  setAccessToken(body.accessToken)
  return body.accessToken
}

interface ApiRequestOptions extends RequestInit {
  auth?: boolean
  retry?: boolean
}

export async function apiRequest<T>(
  path: string,
  options: ApiRequestOptions = {},
): Promise<T> {
  const { auth = true, retry = true, ...requestOptions } = options
  const headers = new Headers(requestOptions.headers)

  if (requestOptions.body && !headers.has('Content-Type')) {
    headers.set('Content-Type', 'application/json')
  }

  const accessToken = getAccessToken()
  if (auth && accessToken) {
    headers.set('Authorization', `Bearer ${accessToken}`)
  }

  const response = await fetch(apiUrl(path), {
    ...requestOptions,
    headers,
    credentials: 'include',
  })

  if (response.status === 401 && auth && retry && accessToken) {
    const refreshedToken = await refreshAccessToken()
    if (refreshedToken) {
      return apiRequest<T>(path, { ...options, retry: false })
    }

    clearAccessToken()
    window.dispatchEvent(new Event(AUTH_UNAUTHORIZED_EVENT))
  }

  if (!response.ok) {
    const error = await readJson<ErrorResponse>(response)
    throw new ApiError(response.status, error)
  }

  if (response.status === 204) return undefined as T
  return (await readJson<T>(response)) as T
}
