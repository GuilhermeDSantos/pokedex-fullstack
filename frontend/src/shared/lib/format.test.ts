import { describe, expect, it } from 'vitest'
import { formatKilograms, formatName, formatPokedexNumber } from './format'

describe('formatName', () => {
  it.each([
    ['pikachu', 'Pikachu'],
    ['lightning-rod', 'Lightning Rod'],
    ['mr-mime', 'Mr Mime'],
  ])('turns PokeAPI slug %s into %s', (slug, name) => {
    expect(formatName(slug)).toBe(name)
  })
})

describe('formatPokedexNumber', () => {
  it.each([
    [1, '#001'],
    [25, '#025'],
    [1025, '#1025'],
  ])('shows %i as %s', (pokedexNumber, label) => {
    expect(formatPokedexNumber(pokedexNumber)).toBe(label)
  })
})

describe('formatKilograms', () => {
  it.each([
    [6, '6.0 kg'],
    [6.9, '6.9 kg'],
    [100, '100.0 kg'],
  ])('shows %f as %s', (kilograms, label) => {
    expect(formatKilograms(kilograms)).toBe(label)
  })
})
