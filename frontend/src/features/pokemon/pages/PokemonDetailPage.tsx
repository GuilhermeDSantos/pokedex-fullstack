import { Link, useParams } from 'react-router'
import { ApiError } from '../../../shared/api/ApiError'
import { formatKilograms, formatName, formatPokedexNumber } from '../../../shared/lib/format'
import { ErrorState } from '../../../shared/ui/ErrorState'
import { Heading } from '../../../shared/ui/Heading'
import { Stack } from '../../../shared/ui/Stack'
import { EvolutionTree } from '../components/EvolutionTree'
import { PokemonStats } from '../components/PokemonStats'
import { usePokemon } from '../hooks/usePokemon'
import styles from './PokemonDetailPage.module.css'

const ARTWORK_SIZE = 240

export function PokemonDetailPage() {
  const { identifier = '' } = useParams()
  const { data: pokemon, error, refetch } = usePokemon(identifier)
  if (error instanceof ApiError && error.status === 404) {
    return (
      <Stack>
        <Heading level={1}>Pokémon not found</Heading>
        <p>
          <Link to="/">Back to the Pokémon list</Link>
        </p>
      </Stack>
    )
  }
  if (error) {
    return <ErrorState message={error.message} onRetry={() => void refetch()} />
  }
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
