import { useEffect, useId, useRef, type KeyboardEvent } from 'react'
import { Button } from './Button'
import styles from './ConfirmDialog.module.css'

type ConfirmDialogProps = {
  title: string
  message: string
  confirmLabel: string
  pending?: boolean
  onConfirm: () => void
  onCancel: () => void
}

export function ConfirmDialog({ title, message, confirmLabel, pending = false, onConfirm, onCancel }: ConfirmDialogProps) {
  const titleId = useId()
  const messageId = useId()
  const dialogRef = useRef<HTMLDivElement>(null)

  // Focus starts on Cancel, the first button: a stray Enter never confirms.
  useEffect(() => {
    dialogRef.current?.querySelector('button')?.focus()
  }, [])

  const handleKeyDown = (event: KeyboardEvent<HTMLDivElement>) => {
    if (event.key === 'Escape') {
      onCancel()
    }
  }

  return (
    <div className={styles.backdrop}>
      <div
        ref={dialogRef}
        role="alertdialog"
        aria-modal="true"
        aria-labelledby={titleId}
        aria-describedby={messageId}
        className={styles.dialog}
        onKeyDown={handleKeyDown}
      >
        <h2 id={titleId} className={styles.title}>
          {title}
        </h2>
        <p id={messageId}>{message}</p>
        <div className={styles.actions}>
          <Button onClick={onCancel}>Cancel</Button>
          <Button pending={pending} onClick={onConfirm}>
            {confirmLabel}
          </Button>
        </div>
      </div>
    </div>
  )
}
