import { useParams } from 'react-router'
import { formatKilograms, formatName, formatPokedexNumber } from '../../../shared/lib/format'
import { Heading } from '../../../shared/ui/Heading'
import { Stack } from '../../../shared/ui/Stack'
import { EvolutionTree } from '../components/EvolutionTree'
import { PokemonStats } from '../components/PokemonStats'
import { usePokemon } from '../hooks/usePokemon'
import styles from './PokemonDetailPage.module.css'

const ARTWORK_SIZE = 240

export function PokemonDetailPage() {
  const { identifier = '' } = useParams()
  const { data: pokemon } = usePokemon(identifier)
  if (!pokemon) {
    return null
  }
  const name = formatName(pokemon.name)
  return (
    <article className={styles.detail}>
      {pokemon.artworkUrl && (
        <img src={pokemon.artworkUrl} alt={`${name} artwork`} width={ARTWORK_SIZE} height={ARTWORK_SIZE} />
      )}
      <Stack>
        <p className={styles.number}>{formatPokedexNumber(pokemon.pokedexNumber)}</p>
        <Heading level={1}>{name}</Heading>
        <p>{pokemon.category}</p>
        <p>{formatKilograms(pokemon.weightKilograms)}</p>
        <p>{pokemon.description}</p>
        <Heading level={2}>Base stats</Heading>
        <PokemonStats stats={pokemon.stats} />
        <Heading level={2}>Evolution</Heading>
        <EvolutionTree root={pokemon.evolutionChain} current={pokemon.name} />
      </Stack>
    </article>
  )
}
