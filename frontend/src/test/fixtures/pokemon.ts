import type { PokemonDetail, PokemonSummary } from '../../features/pokemon/api/pokemonApi'
import type { PageResponse } from '../../shared/api/PageResponse'

export const BULBASAUR: PokemonSummary = {
  pokedexNumber: 1,
  name: 'bulbasaur',
  localizedName: null,
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
  localizedName: null,
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

const STATS = [
  { name: 'HP', value: 35 },
  { name: 'ATTACK', value: 55 },
  { name: 'DEFENSE', value: 40 },
  { name: 'SPECIAL_ATTACK', value: 50 },
  { name: 'SPECIAL_DEFENSE', value: 50 },
  { name: 'SPEED', value: 90 },
]

export const PIKACHU_DETAIL: PokemonDetail = {
  pokedexNumber: 25,
  name: 'pikachu',
  category: 'Mouse Pokémon',
  heightMeters: 0.4,
  weightKilograms: 6,
  spriteUrl: 'https://img.test/25.png',
  artworkUrl: 'https://img.test/25-art.png',
  types: ['electric'],
  abilities: PIKACHU.abilities,
  stats: STATS,
  description: 'Possesses cheek sacs in which it stores electricity.',
  evolutionChain: {
    speciesName: 'pichu',
    pokedexNumber: 172,
    evolvesTo: [
      {
        speciesName: 'pikachu',
        pokedexNumber: 25,
        evolvesTo: [{ speciesName: 'raichu', pokedexNumber: 26, evolvesTo: [] }],
      },
    ],
  },
  local: null,
}

export const SYNCED_PIKACHU_DETAIL: PokemonDetail = {
  ...PIKACHU_DETAIL,
  local: {
    localizedName: 'Pica',
    region: 'Kanto',
    tags: ['mascot', 'starter'],
    syncedAt: '2026-01-15T10:00:00Z',
    updatedAt: '2026-01-15T10:00:00Z',
  },
}

export const EDITED_PIKACHU_DETAIL: PokemonDetail = {
  ...PIKACHU_DETAIL,
  local: {
    localizedName: 'Pikachu BR',
    region: 'Johto',
    tags: ['electric'],
    syncedAt: '2026-01-15T10:00:00Z',
    updatedAt: '2026-01-15T11:00:00Z',
  },
}
