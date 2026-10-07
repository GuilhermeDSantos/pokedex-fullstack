import { useId } from 'react'
import { Link, useLocation } from 'react-router'
import { withReturnTo } from '../../auth/lib/authLink'
import { Heading } from '../../../shared/ui/Heading'
import { Stack } from '../../../shared/ui/Stack'
import type { PokemonDetail } from '../api/pokemonApi'

// Our record of the Pokémon (US-03): what we added on top of the canonical data.
export function LocalSection({ pokemon }: { pokemon: PokemonDetail }) {
  const headingId = useId()
  const { pathname } = useLocation()
  return (
    <section aria-labelledby={headingId}>
      <Stack>
        <Heading level={2}>
          <span id={headingId}>Local data</span>
        </Heading>
        {pokemon.local === null && <Link to={withReturnTo('/login', pathname)}>Log in to sync</Link>}
      </Stack>
    </section>
  )
}
