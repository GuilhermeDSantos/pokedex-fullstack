import styles from './Skeleton.module.css'

// Decorative: the surrounding status region tells assistive tech what is loading.
export function Skeleton({ height }: { height: string }) {
  return <div aria-hidden="true" className={styles.skeleton} style={{ height }} />
}
