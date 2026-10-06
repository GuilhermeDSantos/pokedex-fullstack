const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? '/api/v1'

type RequestOptions = {
  method?: 'GET' | 'POST' | 'PUT' | 'DELETE'
  body?: unknown
}

export async function request<T>(path: string, { method = 'GET', body }: RequestOptions = {}): Promise<T> {
  const headers = new Headers()
  if (body !== undefined) {
    headers.set('Content-Type', 'application/json')
  }
  const response = await fetch(`${API_BASE_URL}${path}`, {
    method,
    headers,
    body: body === undefined ? undefined : JSON.stringify(body),
  })
  return (await response.json()) as T
}
