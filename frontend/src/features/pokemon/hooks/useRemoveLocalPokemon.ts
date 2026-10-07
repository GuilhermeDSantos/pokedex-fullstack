import { useMutation, useQueryClient } from '@tanstack/react-query'
import { useAuth } from '../../auth/useAuth'
import { useSignInAgainOnUnauthenticated } from '../../auth/useSignInAgainOnUnauthenticated'
import { removeLocalPokemon } from '../api/pokemonApi'
import { pokemonKeys } from './pokemonKeys'

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
    onSuccess: () => queryClient.invalidateQueries({ queryKey: pokemonKeys.details() }),
    onError: signInAgainOnUnauthenticated,
  })
}
