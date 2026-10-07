import { useMutation, useQueryClient } from '@tanstack/react-query'
import { useAuth } from '../../auth/useAuth'
import { useSignInAgainOnUnauthenticated } from '../../auth/useSignInAgainOnUnauthenticated'
import { removeLocalPokemon } from '../api/pokemonApi'
import { refreshOurData } from './refreshOurData'

export function useRemoveLocalPokemon(pokedexNumber: number) {
  const { session } = useAuth()
  const queryClient = useQueryClient()
  const signInAgainOnUnauthenticated = useSignInAgainOnUnauthenticated()
  return useMutation({
    mutationFn: () => {
      if (!session) {
        throw new Error('Only a signed-in user can remove')
      }
      return removeLocalPokemon(pokedexNumber, session.accessToken)
    },
    onSuccess: () => refreshOurData(queryClient),
    onError: signInAgainOnUnauthenticated,
  })
}
