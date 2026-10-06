import { request } from '../../../shared/api/httpClient'

export type User = {
  id: string
  email: string
  name: string
  createdAt: string
}

export type AccessToken = {
  accessToken: string
  tokenType: 'Bearer'
  expiresAt: string
}

export type Credentials = {
  email: string
  password: string
}

export type Registration = Credentials & {
  name: string
}

export function register(registration: Registration): Promise<User> {
  return request<User>('/auth/register', { method: 'POST', body: registration })
}

export function login(credentials: Credentials): Promise<AccessToken> {
  return request<AccessToken>('/auth/login', { method: 'POST', body: credentials })
}

export function fetchCurrentUser(accessToken: string): Promise<User> {
  return request<User>('/auth/me', { accessToken })
}
