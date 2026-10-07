import { request } from '../../../shared/api/httpClient'
import type { PageResponse } from '../../../shared/api/PageResponse'

export type Ability = {
  name: string
  hidden: boolean
}

export type PokemonSummary = {
  pokedexNumber: number
  name: string
  spriteUrl: string | null
  category: string | null
  weightKilograms: number
  types: string[]
  abilities: Ability[]
}

export function fetchPokemonPage(page: number, size: number): Promise<PageResponse<PokemonSummary>> {
  return request<PageResponse<PokemonSummary>>(`/pokemon?${new URLSearchParams({ page: String(page), size: String(size) })}`)
}
