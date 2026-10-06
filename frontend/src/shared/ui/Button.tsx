import type { ReactNode } from 'react'
import styles from './Button.module.css'

type ButtonProps = {
  children: ReactNode
  type?: 'button' | 'submit'
  pending?: boolean
  onClick?: () => void
}

export function Button({ children, type, pending = false, onClick }: ButtonProps) {
  return (
    <button type={type} disabled={pending} onClick={onClick} className={styles.button}>
      {children}
    </button>
  )
}
