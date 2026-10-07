import { useId } from 'react'
import { Link, useLocation } from 'react-router'
import { withReturnTo } from '../../auth/lib/authLink'
import { useAuth } from '../../auth/useAuth'
import { Button } from '../../../shared/ui/Button'
import { Heading } from '../../../shared/ui/Heading'
import { Stack } from '../../../shared/ui/Stack'
import type { PokemonDetail } from '../api/pokemonApi'
import { useSyncPokemon } from '../hooks/useSyncPokemon'
import styles from './LocalSection.module.css'

// Our record of the Pokémon (US-03): what we added on top of the canonical data.
export function LocalSection({ pokemon }: { pokemon: PokemonDetail }) {
  const headingId = useId()
  const { pathname } = useLocation()
  const { session } = useAuth()
  const sync = useSyncPokemon(pokemon.name)
  const local = pokemon.local
  return (
    <section aria-labelledby={headingId}>
      <Stack>
        <Heading level={2}>
          <span id={headingId}>Local data</span>
        </Heading>
        {local === null && !session && <Link to={withReturnTo('/login', pathname)}>Log in to sync</Link>}
        {local === null && session && (
          <Button pending={sync.isPending} onClick={() => sync.mutate()}>
            Sync to local database
          </Button>
        )}
        {local !== null && (
          <ul aria-label="Our fields" className={styles.fields}>
            <li>
              <span className={styles.label}>Localized name</span>
              {local.localizedName ?? 'Not set'}
            </li>
            <li>
              <span className={styles.label}>Region</span>
              {local.region ?? 'Not set'}
            </li>
            <li>
              <span className={styles.label}>Tags</span>
              {local.tags.length > 0 ? local.tags.join(', ') : 'None'}
            </li>
          </ul>
        )}
      </Stack>
    </section>
  )
}
