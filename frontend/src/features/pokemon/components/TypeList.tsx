import { formatName } from '../../../shared/lib/format'
import { Badge } from '../../../shared/ui/Badge'
import styles from './InlineList.module.css'

export function TypeList({ types }: { types: string[] }) {
  return (
    <ul aria-label="Types" className={styles.inlineList}>
      {types.map((type) => (
        <li key={type}>
          <Badge>{formatName(type)}</Badge>
        </li>
      ))}
    </ul>
  )
}
