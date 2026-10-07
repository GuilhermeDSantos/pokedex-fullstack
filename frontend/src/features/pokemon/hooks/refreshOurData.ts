import type { QueryClient } from '@tanstack/react-query'
import { pokemonKeys } from './pokemonKeys'

// Our data rides on every detail and on the list's cards. The detail on screen refetches; the cached
// list pages refetch now too, so going back to the list never shows the old values.
export function refreshOurData(queryClient: QueryClient) {
  return Promise.all([
    queryClient.invalidateQueries({ queryKey: pokemonKeys.details() }),
    queryClient.invalidateQueries({ queryKey: pokemonKeys.pages(), refetchType: 'all' }),
  ])
}
