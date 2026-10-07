import { formatName } from '../../../shared/lib/format'
import { Badge } from '../../../shared/ui/Badge'
import styles from './InlineList.module.css'

export function TagList({ tags }: { tags: string[] }) {
  return (
    <ul aria-label="Tags" className={styles.inlineList}>
      {tags.map((tag) => (
        <li key={tag}>
          <Badge>{formatName(tag)}</Badge>
        </li>
      ))}
    </ul>
  )
}
