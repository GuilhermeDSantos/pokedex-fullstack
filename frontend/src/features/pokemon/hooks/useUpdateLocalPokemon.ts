import { useMutation, useQueryClient } from '@tanstack/react-query'
import { useAuth } from '../../auth/useAuth'
import { useSignInAgainOnUnauthenticated } from '../../auth/useSignInAgainOnUnauthenticated'
import { updateLocalPokemon, type LocalEdit } from '../api/pokemonApi'
import { refreshOurData } from './refreshOurData'

export function useUpdateLocalPokemon(pokedexNumber: number) {
  const { session } = useAuth()
  const queryClient = useQueryClient()
  const signInAgainOnUnauthenticated = useSignInAgainOnUnauthenticated()
  return useMutation({
    mutationFn: (edit: LocalEdit) => {
      if (!session) {
        throw new Error('Only a signed-in user can edit')
      }
      return updateLocalPokemon(pokedexNumber, edit, session.accessToken)
    },
    onSuccess: () => refreshOurData(queryClient),
    onError: signInAgainOnUnauthenticated,
  })
}
