import { Heading } from '../../../shared/ui/Heading'
import { Stack } from '../../../shared/ui/Stack'
import { PokemonCard } from '../components/PokemonCard'
import { usePokemonPage } from '../hooks/usePokemonPage'
import styles from './PokemonListPage.module.css'

const PAGE_SIZE = 20

export function PokemonListPage() {
  const { data } = usePokemonPage(0, PAGE_SIZE)
  return (
    <Stack gap={6}>
      <Heading level={1}>Pokémon</Heading>
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
