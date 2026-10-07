import { useQuery } from '@tanstack/react-query'
import { fetchPokemonPage } from '../api/pokemonApi'
import { pokemonKeys } from './pokemonKeys'

export function usePokemonPage(page: number, size: number) {
  return useQuery({
    queryKey: pokemonKeys.page(page, size),
    queryFn: () => fetchPokemonPage(page, size),
  })
}
