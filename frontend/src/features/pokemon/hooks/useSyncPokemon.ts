import { useMutation, useQueryClient } from '@tanstack/react-query'
import { ApiError } from '../../../shared/api/ApiError'
import { useAuth } from '../../auth/useAuth'
import { useSignInAgainOnUnauthenticated } from '../../auth/useSignInAgainOnUnauthenticated'
import { syncPokemon } from '../api/pokemonApi'
import { pokemonKeys } from './pokemonKeys'

export function useSyncPokemon(pokedexNumber: number) {
  const { session } = useAuth()
  const queryClient = useQueryClient()
  const signInAgainOnUnauthenticated = useSignInAgainOnUnauthenticated()
  return useMutation({
    mutationFn: () => {
      if (!session) {
        throw new Error('Only a signed-in user can sync')
      }
      return syncPokemon(pokedexNumber, session.accessToken)
    },
    // The detail carries our fields, so it is read again. Every cached detail: the same Pokémon may be
    // cached under its name and its number.
    onSuccess: () => queryClient.invalidateQueries({ queryKey: pokemonKeys.details() }),
    // A 409 means the record exists after all: read it, and the page explains what happened.
    onError: (error) => {
      if (error instanceof ApiError && error.status === 409) {
        void queryClient.invalidateQueries({ queryKey: pokemonKeys.details() })
      }
      signInAgainOnUnauthenticated(error)
    },
  })
}
