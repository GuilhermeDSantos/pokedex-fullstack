import { describe, expect, it } from 'vitest'
import { safeReturnTo } from './returnTo'

describe('safeReturnTo', () => {
  it('keeps a path inside the app', () => {
    expect(safeReturnTo('/pokemon/25?tab=local')).toBe('/pokemon/25?tab=local')
  })

  it('falls back to the start page when there is no returnTo', () => {
    expect(safeReturnTo(null)).toBe('/')
  })

  it.each(['https://evil.example/phish', '//evil.example/phish', 'javascript:alert(1)', 'pokemon/25'])(
    'refuses %s, so signing in can never send the user to another site',
    (value) => {
      expect(safeReturnTo(value)).toBe('/')
    },
  )

  it('refuses a backslash after the first slash, which browsers read as "//"', () => {
    expect(safeReturnTo('/\\evil.example/phish')).toBe('/')
  })
})
