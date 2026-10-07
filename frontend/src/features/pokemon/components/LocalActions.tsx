import { useState } from 'react'
import { Link, useLocation } from 'react-router'
import { withReturnTo } from '../../auth/lib/authLink'
import { useAuth } from '../../auth/useAuth'
import { ApiError } from '../../../shared/api/ApiError'
import { formatName } from '../../../shared/lib/format'
import { Button } from '../../../shared/ui/Button'
import { ConfirmDialog } from '../../../shared/ui/ConfirmDialog'
import type { PokemonDetail } from '../api/pokemonApi'
import { useRemoveLocalPokemon } from '../hooks/useRemoveLocalPokemon'
import { useSyncPokemon } from '../hooks/useSyncPokemon'
import { LocalForm } from './LocalForm'

export function LocalActions({ pokemon }: { pokemon: PokemonDetail }) {
  const { pathname } = useLocation()
  const { session } = useAuth()
  const sync = useSyncPokemon(pokemon.pokedexNumber)
  const syncedMeanwhile = sync.error instanceof ApiError && sync.error.status === 409
  const [editing, setEditing] = useState(false)
  const remove = useRemoveLocalPokemon(pokemon.pokedexNumber)
  const [confirmingRemoval, setConfirmingRemoval] = useState(false)
  return (
    <>
      {pokemon.local === null && !session && <Link to={withReturnTo('/login', pathname)}>Log in to sync</Link>}
      {pokemon.local === null && session && (
        <Button pending={sync.isPending} onClick={() => sync.mutate()}>
          Sync to local database
        </Button>
      )}
      {syncedMeanwhile && <p role="status">Someone synced this Pokémon just before you.</p>}
      {pokemon.local !== null && session && !editing && (
        <>
          <Button onClick={() => setEditing(true)}>Edit</Button>
          <Button onClick={() => setConfirmingRemoval(true)}>Remove</Button>
        </>
      )}
      {confirmingRemoval && (
        <ConfirmDialog
          title={`Remove our record of ${formatName(pokemon.name)}?`}
          message="Its localized name, region and tags are deleted. The Pokémon stays in the catalog."
          confirmLabel="Remove"
          pending={remove.isPending}
          onConfirm={() => remove.mutate(undefined, { onSuccess: () => setConfirmingRemoval(false) })}
          onCancel={() => setConfirmingRemoval(false)}
        />
      )}
      {pokemon.local !== null && session && editing && (
        <LocalForm pokedexNumber={pokemon.pokedexNumber} local={pokemon.local} onDone={() => setEditing(false)} />
      )}
    </>
  )
}
