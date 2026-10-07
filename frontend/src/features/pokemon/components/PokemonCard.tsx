import { useId } from 'react'
import { Link } from 'react-router'
import { formatKilograms, formatName, formatPokedexNumber } from '../../../shared/lib/format'
import { Card } from '../../../shared/ui/Card'
import type { PokemonSummary } from '../api/pokemonApi'
import { AbilityList } from './AbilityList'
import styles from './PokemonCard.module.css'
import { BadgeList } from './BadgeList'

const SPRITE_SIZE = 96

export function PokemonCard({ pokemon }: { pokemon: PokemonSummary }) {
  const titleId = useId()
  const name = formatName(pokemon.name)
  return (
    <Card labelledBy={titleId}>
      {pokemon.spriteUrl ? (
        <img
          src={pokemon.spriteUrl}
          alt={`${name} sprite`}
          width={SPRITE_SIZE}
          height={SPRITE_SIZE}
          loading="lazy"
          className={styles.sprite}
        />
      ) : (
        <p className={styles.noSprite}>No image</p>
      )}
      <p className={styles.number}>{formatPokedexNumber(pokemon.pokedexNumber)}</p>
      <h2 id={titleId} className={styles.name}>
        <Link to={`/pokemon/${pokemon.name}`}>{name}</Link>
      </h2>
      {pokemon.localizedName && <p className={styles.localizedName}>{pokemon.localizedName}</p>}
      <p>{pokemon.category ?? 'Category unknown'}</p>
      <p>{formatKilograms(pokemon.weightKilograms)}</p>
      <BadgeList label="Types" items={pokemon.types} />
      <AbilityList abilities={pokemon.abilities} />
    </Card>
  )
}
