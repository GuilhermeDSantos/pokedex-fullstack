import { useEffect, useId, useRef, type KeyboardEvent, type ReactNode } from 'react'
import styles from './Dialog.module.css'

type DialogProps = {
  title: string
  description?: string
  role?: 'dialog' | 'alertdialog'
  onClose: () => void
  children: ReactNode
}

export function Dialog({ title, description, role = 'dialog', onClose, children }: DialogProps) {
  const titleId = useId()
  const descriptionId = useId()
  const dialogRef = useRef<HTMLDivElement>(null)

  // Focus starts on the first control: the first field of a form, or Cancel in a confirmation.
  useEffect(() => {
    dialogRef.current?.querySelector<HTMLElement>('input, button')?.focus()
  }, [])

  const handleKeyDown = (event: KeyboardEvent<HTMLDivElement>) => {
    if (event.key === 'Escape') {
      onClose()
    }
  }

  return (
    <div className={styles.backdrop}>
      <div
        ref={dialogRef}
        role={role}
        aria-modal="true"
        aria-labelledby={titleId}
        aria-describedby={description ? descriptionId : undefined}
        className={styles.dialog}
        onKeyDown={handleKeyDown}
      >
        <h2 id={titleId} className={styles.title}>
          {title}
        </h2>
        {description && <p id={descriptionId}>{description}</p>}
        {children}
      </div>
    </div>
  )
}
