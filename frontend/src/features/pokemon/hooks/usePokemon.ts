import { useQuery } from '@tanstack/react-query'
import { fetchPokemon } from '../api/pokemonApi'
import { pokemonKeys } from './pokemonKeys'

export function usePokemon(identifier: string) {
  return useQuery({
    queryKey: pokemonKeys.detail(identifier),
    queryFn: () => fetchPokemon(identifier),
  })
}
