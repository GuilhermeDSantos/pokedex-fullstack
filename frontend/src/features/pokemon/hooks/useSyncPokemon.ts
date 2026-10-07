import { useMutation, useQueryClient } from '@tanstack/react-query'
import { useLocation, useNavigate } from 'react-router'
import { ApiError } from '../../../shared/api/ApiError'
import { withReturnTo } from '../../auth/lib/authLink'
import { useAuth } from '../../auth/useAuth'
import { syncPokemon } from '../api/pokemonApi'
import { pokemonKeys } from './pokemonKeys'

export function useSyncPokemon(pokedexNumber: number) {
  const { session, signOut } = useAuth()
  const queryClient = useQueryClient()
  const navigate = useNavigate()
  const { pathname } = useLocation()
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
    // A 409 means the record exists after all: read it, and the section explains what happened.
    onError: (error) => {
      if (error instanceof ApiError && error.status === 409) {
        void queryClient.invalidateQueries({ queryKey: pokemonKeys.details() })
      }
      // The token expired or the account is gone (D-033): end the session and come back after signing in.
      if (error instanceof ApiError && error.status === 401) {
        signOut()
        void navigate(withReturnTo('/login', pathname))
      }
    },
  })
}
