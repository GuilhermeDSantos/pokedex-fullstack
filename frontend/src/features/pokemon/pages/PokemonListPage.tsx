import { Heading } from '../../../shared/ui/Heading'
import { Skeleton } from '../../../shared/ui/Skeleton'
import { Stack } from '../../../shared/ui/Stack'
import { PokemonCard } from '../components/PokemonCard'
import { usePokemonPage } from '../hooks/usePokemonPage'
import styles from './PokemonListPage.module.css'

const PAGE_SIZE = 20

export function PokemonListPage() {
  const { data, isPending } = usePokemonPage(0, PAGE_SIZE)
  return (
    <Stack gap={6}>
      <Heading level={1}>Pokémon</Heading>
      {isPending && (
        <div role="status" aria-label="Loading Pokémon">
          <ul className={styles.grid}>
            {Array.from({ length: PAGE_SIZE }, (_, slot) => (
              <li key={slot}>
                <Skeleton variant="card" />
              </li>
            ))}
          </ul>
        </div>
      )}
      {data && (
        <ul className={styles.grid}>
          {data.content.map((pokemon) => (
            <li key={pokemon.pokedexNumber}>
              <PokemonCard pokemon={pokemon} />
            </li>
          ))}
        </ul>
      )}
    </Stack>
  )
}
