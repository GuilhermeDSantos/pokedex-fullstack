import { useId } from 'react'
import { Link } from 'react-router'
import { formatKilograms, formatName, formatPokedexNumber } from '../../../shared/lib/format'
import { Badge } from '../../../shared/ui/Badge'
import { Card } from '../../../shared/ui/Card'
import type { PokemonSummary } from '../api/pokemonApi'
import styles from './PokemonCard.module.css'

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
      <p>{pokemon.category ?? 'Category unknown'}</p>
      <p>{formatKilograms(pokemon.weightKilograms)}</p>
      <ul aria-label="Types" className={styles.types}>
        {pokemon.types.map((type) => (
          <li key={type}>
            <Badge>{formatName(type)}</Badge>
          </li>
        ))}
      </ul>
      <ul aria-label="Abilities" className={styles.abilities}>
        {pokemon.abilities.map((ability) => (
          <li key={ability.name}>
            {formatName(ability.name)}
            {ability.hidden && ' (hidden)'}
          </li>
        ))}
      </ul>
    </Card>
  )
}
