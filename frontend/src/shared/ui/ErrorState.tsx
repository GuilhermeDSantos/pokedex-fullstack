import { Button } from './Button'
import styles from './ErrorState.module.css'

type ErrorStateProps = {
  message: string
  onRetry: () => void
}

export function ErrorState({ message, onRetry }: ErrorStateProps) {
  return (
    <div role="alert" className={styles.errorState}>
      <p className={styles.message}>{message}</p>
      <Button onClick={onRetry}>Retry</Button>
    </div>
  )
}
