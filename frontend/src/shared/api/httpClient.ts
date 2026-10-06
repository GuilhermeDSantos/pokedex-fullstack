import { ApiError } from './ApiError'
import type { ErrorResponse } from './ErrorResponse'

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? '/api/v1'

type RequestOptions = {
  method?: 'GET' | 'POST' | 'PUT' | 'DELETE'
  body?: unknown
  accessToken?: string
}

export async function request<T>(path: string, { method = 'GET', body, accessToken }: RequestOptions = {}): Promise<T> {
  const headers = new Headers()
  if (body !== undefined) {
    headers.set('Content-Type', 'application/json')
  }
  if (accessToken !== undefined) {
    headers.set('Authorization', `Bearer ${accessToken}`)
  }
  const response = await fetch(`${API_BASE_URL}${path}`, {
    method,
    headers,
    body: body === undefined ? undefined : JSON.stringify(body),
  })
  if (!response.ok) {
    const error = (await response.json()) as ErrorResponse
    throw new ApiError(response.status, error.code, error.message, error.fieldErrors)
  }
  return (await response.json()) as T
}
