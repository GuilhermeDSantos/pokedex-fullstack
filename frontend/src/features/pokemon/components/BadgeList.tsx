import { formatName } from '../../../shared/lib/format'
import { Badge } from '../../../shared/ui/Badge'
import styles from './InlineList.module.css'

export function BadgeList({ label, items }: { label: string; items: string[] }) {
  return (
    <ul aria-label={label} className={styles.inlineList}>
      {items.map((item) => (
        <li key={item}>
          <Badge>{formatName(item)}</Badge>
        </li>
      ))}
    </ul>
  )
}
