import { Link } from 'react-router'
import { formatName } from '../../../shared/lib/format'
import type { EvolutionStage } from '../api/pokemonApi'
import styles from './EvolutionTree.module.css'

type EvolutionTreeProps = {
  root: EvolutionStage
  current: string
}

// Nested lists, because lineages branch: Eevee has eight children on one level.
export function EvolutionTree({ root, current }: EvolutionTreeProps) {
  return (
    <nav aria-label="Evolution">
      <ul className={styles.tree}>
        <Stage stage={root} current={current} />
      </ul>
    </nav>
  )
}

function Stage({ stage, current }: { stage: EvolutionStage; current: string }) {
  return (
    <li>
      <Link
        to={`/pokemon/${stage.speciesName}`}
        aria-current={stage.speciesName === current ? 'page' : undefined}
        className={styles.stage}
      >
        {formatName(stage.speciesName)}
      </Link>
      {stage.evolvesTo.length > 0 && (
        <ul className={styles.tree}>
          {stage.evolvesTo.map((next) => (
            <Stage key={next.pokedexNumber} stage={next} current={current} />
          ))}
        </ul>
      )}
    </li>
  )
}
