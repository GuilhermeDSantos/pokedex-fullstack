import { describe, expect, it } from 'vitest'
import { ApiError } from '../shared/api/ApiError'
import { shouldRetry } from './retryPolicy'

describe('shouldRetry', () => {
  // A 404 or a 400 answers the same every time: retrying only delays the not-found or error state.
  it.each([400, 401, 404, 409])('never retries a %i', (status) => {
    expect(shouldRetry(0, new ApiError(status, 'ANY', 'any'))).toBe(false)
  })

  // How many times to retry a 5xx is tuned in the UI pass (U.4); for now it keeps the library's three.
  it('retries a server or network failure up to three times', () => {
    expect(shouldRetry(2, new ApiError(503, 'SOURCE_UNAVAILABLE', 'down'))).toBe(true)
    expect(shouldRetry(3, new ApiError(503, 'SOURCE_UNAVAILABLE', 'down'))).toBe(false)
    expect(shouldRetry(0, new ApiError(0, 'NETWORK_ERROR', 'offline'))).toBe(true)
  })
})
