import type { ReactNode } from 'react'
import styles from './Stack.module.css'

type StackProps = {
  gap?: 2 | 4 | 6
  children: ReactNode
}

export function Stack({ gap = 4, children }: StackProps) {
  return <div className={`${styles.stack} ${styles[`gap${gap}`]}`}>{children}</div>
}
