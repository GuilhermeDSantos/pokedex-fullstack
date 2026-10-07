import type { PokemonSummary } from '../../features/pokemon/api/pokemonApi'
import type { PageResponse } from '../../shared/api/PageResponse'

export const BULBASAUR: PokemonSummary = {
  pokedexNumber: 1,
  name: 'bulbasaur',
  spriteUrl: 'https://img.test/1.png',
  category: 'Seed Pokémon',
  weightKilograms: 6.9,
  types: ['grass', 'poison'],
  abilities: [
    { name: 'overgrow', hidden: false },
    { name: 'chlorophyll', hidden: true },
  ],
}

export const PIKACHU: PokemonSummary = {
  pokedexNumber: 25,
  name: 'pikachu',
  spriteUrl: 'https://img.test/25.png',
  category: 'Mouse Pokémon',
  weightKilograms: 6,
  types: ['electric'],
  abilities: [
    { name: 'static', hidden: false },
    { name: 'lightning-rod', hidden: true },
  ],
}

export function pageOf<T>(content: T[], { page = 0, size = 20, totalElements = 1351 } = {}): PageResponse<T> {
  return { content, page, size, totalElements, totalPages: Math.ceil(totalElements / size) }
}
