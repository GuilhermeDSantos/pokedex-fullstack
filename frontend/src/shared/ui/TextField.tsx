import { useId } from 'react'
import styles from './TextField.module.css'

type TextFieldProps = {
  label: string
  name: string
  value: string
  onChange: (value: string) => void
  type?: 'text' | 'email' | 'password'
  autoComplete?: string
  error?: string
}

export function TextField({ label, name, value, onChange, type = 'text', autoComplete }: TextFieldProps) {
  const inputId = useId()
  return (
    <div className={styles.field}>
      <label htmlFor={inputId} className={styles.label}>
        {label}
      </label>
      <input
        id={inputId}
        name={name}
        type={type}
        value={value}
        autoComplete={autoComplete}
        onChange={(event) => onChange(event.target.value)}
        className={styles.input}
      />
    </div>
  )
}
