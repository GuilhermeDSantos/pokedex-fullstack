import styles from './FormError.module.css'

export function FormError({ message }: { message: string }) {
  return (
    <p role="alert" className={styles.formError}>
      {message}
    </p>
  )
}
