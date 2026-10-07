import { useMutation, useQueryClient } from '@tanstack/react-query'
import { useLocation, useNavigate } from 'react-router'
import { ApiError } from '../../../shared/api/ApiError'
import { withReturnTo } from '../../auth/lib/authLink'
import { useAuth } from '../../auth/useAuth'
import { removeLocalPokemon } from '../api/pokemonApi'
import { pokemonKeys } from './pokemonKeys'

export function useRemoveLocalPokemon(pokedexNumber: number) {
  const { session, signOut } = useAuth()
  const queryClient = useQueryClient()
  const navigate = useNavigate()
  const { pathname } = useLocation()
  return useMutation({
    mutationFn: () => {
      if (!session) {
        throw new Error('Only a signed-in user can remove')
      }
      return removeLocalPokemon(pokedexNumber, session.accessToken)
    },
    onSuccess: () => queryClient.invalidateQueries({ queryKey: pokemonKeys.details() }),
    onError: (error) => {
      if (error instanceof ApiError && error.status === 401) {
        signOut()
        void navigate(withReturnTo('/login', pathname))
      }
    },
  })
}
