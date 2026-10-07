import { Link, useParams } from 'react-router'
import { ApiError } from '../../../shared/api/ApiError'
import { formatKilograms, formatMeters, formatName, formatPokedexNumber } from '../../../shared/lib/format'
import { ErrorState } from '../../../shared/ui/ErrorState'
import { Heading } from '../../../shared/ui/Heading'
import { Skeleton } from '../../../shared/ui/Skeleton'
import { Stack } from '../../../shared/ui/Stack'
import { AbilityList } from '../components/AbilityList'
import { EvolutionTree } from '../components/EvolutionTree'
import { LocalSection } from '../components/LocalSection'
import { PokemonStats } from '../components/PokemonStats'
import { TypeList } from '../components/TypeList'
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
    return (
      <div role="status" aria-label="Loading Pokémon">
        <Skeleton variant="detail" />
      </div>
    )
  }
  const name = formatName(pokemon.name)
  return (
    <article className={styles.detail}>
      {pokemon.artworkUrl ? (
        <img src={pokemon.artworkUrl} alt={`${name} artwork`} width={ARTWORK_SIZE} height={ARTWORK_SIZE} />
      ) : pokemon.spriteUrl ? (
        <img src={pokemon.spriteUrl} alt={`${name} sprite`} width={ARTWORK_SIZE} height={ARTWORK_SIZE} />
      ) : (
        <p className={styles.noImage}>No image</p>
      )}
      <Stack>
        <p className={styles.number}>{formatPokedexNumber(pokemon.pokedexNumber)}</p>
        <Heading level={1}>{pokemon.local?.localizedName ?? name}</Heading>
        {pokemon.local?.localizedName && <p className={styles.number}>Originally {name}</p>}
        <TypeList types={pokemon.types} />
        <p>{pokemon.description}</p>
        <ul aria-label="Facts" className={styles.facts}>
          <li>
            <span className={styles.label}>Category</span>
            {pokemon.category ?? 'Category unknown'}
          </li>
          <li>
            <span className={styles.label}>Height</span>
            {formatMeters(pokemon.heightMeters)}
          </li>
          <li>
            <span className={styles.label}>Weight</span>
            {formatKilograms(pokemon.weightKilograms)}
          </li>
        </ul>
        <Heading level={2}>Abilities</Heading>
        <AbilityList abilities={pokemon.abilities} />
        <Heading level={2}>Base stats</Heading>
        <PokemonStats stats={pokemon.stats} />
        <Heading level={2}>Evolution</Heading>
        <EvolutionTree root={pokemon.evolutionChain} current={pokemon.name} />
        <LocalSection pokemon={pokemon} />
      </Stack>
    </article>
  )
}
