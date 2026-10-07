import type { Stat } from '../api/pokemonApi'
import styles from './PokemonStats.module.css'

// The API names stats as the domain does (SPECIAL_ATTACK); these are the games' labels.
const LABELS: Record<string, string> = {
  HP: 'HP',
  ATTACK: 'Attack',
  DEFENSE: 'Defense',
  SPECIAL_ATTACK: 'Sp. Atk',
  SPECIAL_DEFENSE: 'Sp. Def',
  SPEED: 'Speed',
}

export function PokemonStats({ stats }: { stats: Stat[] }) {
  return (
    <ul aria-label="Base stats" className={styles.stats}>
      {stats.map((stat) => (
        <li key={stat.name} className={styles.stat}>
          <span>{LABELS[stat.name] ?? stat.name}</span>
          <span className={styles.value}>{stat.value}</span>
        </li>
      ))}
    </ul>
  )
}
