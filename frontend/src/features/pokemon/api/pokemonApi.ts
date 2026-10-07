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

export type Stat = {
  name: string
  value: number
}

export type EvolutionStage = {
  speciesName: string
  pokedexNumber: number
  evolvesTo: EvolutionStage[]
}

export type PokemonDetail = {
  pokedexNumber: number
  name: string
  category: string | null
  heightMeters: number
  weightKilograms: number
  spriteUrl: string | null
  artworkUrl: string | null
  types: string[]
  abilities: Ability[]
  stats: Stat[]
  description: string | null
  evolutionChain: EvolutionStage
}

export function fetchPokemon(identifier: string): Promise<PokemonDetail> {
  return request<PokemonDetail>(`/pokemon/${encodeURIComponent(identifier)}`)
}

export function fetchPokemonPage(page: number, size: number): Promise<PageResponse<PokemonSummary>> {
  return request<PageResponse<PokemonSummary>>(`/pokemon?${new URLSearchParams({ page: String(page), size: String(size) })}`)
}
