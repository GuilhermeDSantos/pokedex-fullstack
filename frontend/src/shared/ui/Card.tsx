import type { ReactNode } from 'react'
import styles from './Card.module.css'

type CardProps = {
  labelledBy: string
  children: ReactNode
}

export function Card({ labelledBy, children }: CardProps) {
  return (
    <article aria-labelledby={labelledBy} className={styles.card}>
      {children}
    </article>
  )
}
