import { formatName } from '../../../shared/lib/format'
import type { Ability } from '../api/pokemonApi'
import styles from './InlineList.module.css'

// The brief's "skills" (D-010).
export function AbilityList({ abilities }: { abilities: Ability[] }) {
  return (
    <ul aria-label="Abilities" className={styles.inlineList}>
      {abilities.map((ability) => (
        <li key={ability.name}>
          {formatName(ability.name)}
          {ability.hidden && ' (hidden)'}
        </li>
      ))}
    </ul>
  )
}
