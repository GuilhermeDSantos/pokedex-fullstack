import { ApiError } from '../shared/api/ApiError'

const DEFAULT_RETRIES = 3

// A 4xx answers the same every time, so only server and network failures are worth another try.
export function shouldRetry(failureCount: number, error: Error): boolean {
  if (error instanceof ApiError && error.status >= 400 && error.status < 500) {
    return false
  }
  return failureCount < DEFAULT_RETRIES
}
