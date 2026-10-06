const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? '/api/v1'

export async function request<T>(path: string): Promise<T> {
  const response = await fetch(`${API_BASE_URL}${path}`)
  return (await response.json()) as T
}
