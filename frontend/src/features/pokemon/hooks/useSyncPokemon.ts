import { useMutation, useQueryClient } from '@tanstack/react-query'
import { ApiError } from '../../../shared/api/ApiError'
import { useAuth } from '../../auth/useAuth'
import { useSignInAgainOnUnauthenticated } from '../../auth/useSignInAgainOnUnauthenticated'
import { syncPokemon } from '../api/pokemonApi'
import { refreshOurData } from './refreshOurData'

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
    onSuccess: () => refreshOurData(queryClient),
    // A 409 means the record exists after all: read it, and the page explains what happened.
    onError: (error) => {
      if (error instanceof ApiError && error.status === 409) {
        void refreshOurData(queryClient)
      }
      signInAgainOnUnauthenticated(error)
    },
  })
}
