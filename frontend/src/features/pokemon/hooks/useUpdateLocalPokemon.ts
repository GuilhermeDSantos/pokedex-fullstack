import { useMutation, useQueryClient } from '@tanstack/react-query'
import { useLocation, useNavigate } from 'react-router'
import { ApiError } from '../../../shared/api/ApiError'
import { withReturnTo } from '../../auth/lib/authLink'
import { useAuth } from '../../auth/useAuth'
import { updateLocalPokemon, type LocalEdit } from '../api/pokemonApi'
import { pokemonKeys } from './pokemonKeys'

export function useUpdateLocalPokemon(pokedexNumber: number) {
  const { session, signOut } = useAuth()
  const queryClient = useQueryClient()
  const navigate = useNavigate()
  const { pathname } = useLocation()
  return useMutation({
    mutationFn: (edit: LocalEdit) => {
      if (!session) {
        throw new Error('Only a signed-in user can edit')
      }
      return updateLocalPokemon(pokedexNumber, edit, session.accessToken)
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
