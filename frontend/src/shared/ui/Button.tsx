import type { ReactNode } from 'react'
import styles from './Button.module.css'

type ButtonProps = {
  children: ReactNode
  type?: 'button' | 'submit'
  variant?: 'primary' | 'secondary' | 'danger'
  pending?: boolean
  onClick?: () => void
}

export function Button({ children, type = 'button', variant = 'primary', pending = false, onClick }: ButtonProps) {
  return (
    <button type={type} disabled={pending} onClick={onClick} className={`${styles.button} ${styles[variant]}`}>
      {children}
    </button>
  )
}
