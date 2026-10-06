import type { ReactNode } from 'react'
import styles from './Heading.module.css'

type HeadingProps = {
  level: 1 | 2 | 3
  children: ReactNode
}

export function Heading({ level, children }: HeadingProps) {
  const Tag = `h${level}` as const
  return <Tag className={styles[`level${level}`]}>{children}</Tag>
}
