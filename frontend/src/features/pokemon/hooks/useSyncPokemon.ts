import { useMutation, useQueryClient } from '@tanstack/react-query'
import { useAuth } from '../../auth/useAuth'
import { syncPokemon } from '../api/pokemonApi'
import { pokemonKeys } from './pokemonKeys'

export function useSyncPokemon(identifier: string) {
  const { session } = useAuth()
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: () => {
      if (!session) {
        throw new Error('Only a signed-in user can sync')
      }
      return syncPokemon(identifier, session.accessToken)
    },
    // The detail carries the local part, so it is read again rather than patched by hand.
    onSuccess: () => queryClient.invalidateQueries({ queryKey: pokemonKeys.detail(identifier) }),
  })
}
