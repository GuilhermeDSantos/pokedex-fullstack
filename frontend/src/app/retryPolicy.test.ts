import { describe, expect, it } from 'vitest'
import { ApiError } from '../shared/api/ApiError'
import { shouldRetry } from './retryPolicy'

describe('shouldRetry', () => {
  // A 404 or a 400 answers the same every time: retrying only delays the not-found or error state.
  it.each([400, 401, 404, 409])('never retries a %i', (status) => {
    expect(shouldRetry(0, new ApiError(status, 'ANY', 'any'))).toBe(false)
  })
})
