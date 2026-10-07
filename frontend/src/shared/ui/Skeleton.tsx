import styles from './Skeleton.module.css'

type SkeletonProps = {
  variant: 'card'
}

// Decorative: the surrounding status region tells assistive tech what is loading.
export function Skeleton({ variant }: SkeletonProps) {
  return <div aria-hidden="true" className={`${styles.skeleton} ${styles[variant]}`} />
}
