import type { TokenResponse, UserInfo } from '../types/api'
import { apiRequest } from './client'

export function login(loginId: string, password: string) {
  return apiRequest<TokenResponse>('/api/users/login', {
    method: 'POST',
    auth: false,
    body: JSON.stringify({ loginId, password }),
  })
}

export function logout() {
  return apiRequest<void>('/api/users/logout', { method: 'POST' })
}

export function getMyInfo() {
  return apiRequest<UserInfo>('/api/users/info')
}
