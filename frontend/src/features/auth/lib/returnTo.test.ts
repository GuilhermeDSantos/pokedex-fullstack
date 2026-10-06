import { describe, expect, it } from 'vitest'
import { safeReturnTo } from './returnTo'

describe('safeReturnTo', () => {
  it('keeps a path inside the app', () => {
    expect(safeReturnTo('/pokemon/25?tab=local')).toBe('/pokemon/25?tab=local')
  })

  it('falls back to the start page when there is no returnTo', () => {
    expect(safeReturnTo(null)).toBe('/')
  })
})
