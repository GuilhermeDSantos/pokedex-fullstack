import { describe, expect, it } from 'vitest'
import { formatName } from './format'

describe('formatName', () => {
  it.each([
    ['pikachu', 'Pikachu'],
    ['lightning-rod', 'Lightning Rod'],
    ['mr-mime', 'Mr Mime'],
  ])('turns PokeAPI slug %s into %s', (slug, name) => {
    expect(formatName(slug)).toBe(name)
  })
})
