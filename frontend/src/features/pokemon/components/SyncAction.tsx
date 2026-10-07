import { Link, useLocation } from 'react-router'
import { withReturnTo } from '../../auth/lib/authLink'
import { useAuth } from '../../auth/useAuth'
import { ApiError } from '../../../shared/api/ApiError'
import { Button } from '../../../shared/ui/Button'
import type { PokemonDetail } from '../api/pokemonApi'
import { useSyncPokemon } from '../hooks/useSyncPokemon'

export function SyncAction({ pokemon }: { pokemon: PokemonDetail }) {
  const { pathname } = useLocation()
  const { session } = useAuth()
  const sync = useSyncPokemon(pokemon.pokedexNumber)
  const syncedMeanwhile = sync.error instanceof ApiError && sync.error.status === 409
  return (
    <>
      {pokemon.local === null && !session && <Link to={withReturnTo('/login', pathname)}>Log in to sync</Link>}
      {pokemon.local === null && session && (
        <Button pending={sync.isPending} onClick={() => sync.mutate()}>
          Sync to local database
        </Button>
      )}
      {syncedMeanwhile && <p role="status">Someone synced this Pokémon just before you.</p>}
    </>
  )
}
