import { ApiError } from './ApiError'
import type { ErrorResponse } from './ErrorResponse'

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? '/api/v1'
const UNEXPECTED_RESPONSE_MESSAGE = 'Something went wrong. Please try again.'
const NETWORK_ERROR_MESSAGE = 'Could not reach the server. Check your connection and try again.'

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
  const response = await send(`${API_BASE_URL}${path}`, {
    method,
    headers,
    body: body === undefined ? undefined : JSON.stringify(body),
  })
  if (!response.ok) {
    throw await toApiError(response)
  }
  return (await response.json()) as T
}

async function send(url: string, init: RequestInit): Promise<Response> {
  try {
    return await fetch(url, init)
  } catch (error) {
    // fetch rejects with a TypeError only when no response arrived at all.
    if (error instanceof TypeError) {
      throw new ApiError(0, 'NETWORK_ERROR', NETWORK_ERROR_MESSAGE)
    }
    throw error
  }
}

async function toApiError(response: Response): Promise<ApiError> {
  const body: unknown = await response.json().catch(() => null)
  if (isErrorResponse(body)) {
    return new ApiError(response.status, body.code, body.message, body.fieldErrors)
  }
  // A proxy in front of the backend (nginx) answers with HTML when the backend is down.
  return new ApiError(response.status, 'UNEXPECTED_RESPONSE', UNEXPECTED_RESPONSE_MESSAGE)
}

function isErrorResponse(body: unknown): body is ErrorResponse {
  return (
    typeof body === 'object' &&
    body !== null &&
    'code' in body &&
    typeof body.code === 'string' &&
    'message' in body &&
    typeof body.message === 'string' &&
    'fieldErrors' in body &&
    Array.isArray(body.fieldErrors)
  )
}
