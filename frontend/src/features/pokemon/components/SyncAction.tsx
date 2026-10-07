import { useState } from 'react'
import { Link, useLocation } from 'react-router'
import { withReturnTo } from '../../auth/lib/authLink'
import { useAuth } from '../../auth/useAuth'
import { ApiError } from '../../../shared/api/ApiError'
import { Button } from '../../../shared/ui/Button'
import type { PokemonDetail } from '../api/pokemonApi'
import { useSyncPokemon } from '../hooks/useSyncPokemon'
import { LocalForm } from './LocalForm'

export function SyncAction({ pokemon }: { pokemon: PokemonDetail }) {
  const { pathname } = useLocation()
  const { session } = useAuth()
  const sync = useSyncPokemon(pokemon.pokedexNumber)
  const syncedMeanwhile = sync.error instanceof ApiError && sync.error.status === 409
  const [editing, setEditing] = useState(false)
  return (
    <>
      {pokemon.local === null && !session && <Link to={withReturnTo('/login', pathname)}>Log in to sync</Link>}
      {pokemon.local === null && session && (
        <Button pending={sync.isPending} onClick={() => sync.mutate()}>
          Sync to local database
        </Button>
      )}
      {syncedMeanwhile && <p role="status">Someone synced this Pokémon just before you.</p>}
      {pokemon.local !== null && session && !editing && <Button onClick={() => setEditing(true)}>Edit</Button>}
      {pokemon.local !== null && session && editing && (
        <LocalForm pokedexNumber={pokemon.pokedexNumber} local={pokemon.local} onDone={() => setEditing(false)} />
      )}
    </>
  )
}
