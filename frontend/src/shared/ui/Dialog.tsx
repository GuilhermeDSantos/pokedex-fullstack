import { useEffect, useId, useRef, type KeyboardEvent, type ReactNode } from 'react'
import styles from './Dialog.module.css'

const FOCUSABLE = 'input, select, textarea, button, a[href]'

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
    dialogRef.current?.querySelector<HTMLElement>(FOCUSABLE)?.focus()
  }, [])

  const handleKeyDown = (event: KeyboardEvent<HTMLDivElement>) => {
    if (event.key === 'Escape') {
      onClose()
    }
    if (event.key === 'Tab') {
      keepFocusInside(event)
    }
  }

  // A modal keeps the keyboard inside: Tab wraps around instead of reaching the page behind.
  const keepFocusInside = (event: KeyboardEvent<HTMLDivElement>) => {
    const controls = Array.from(dialogRef.current?.querySelectorAll<HTMLElement>(FOCUSABLE) ?? [])
    const first = controls.at(0)
    const last = controls.at(-1)
    if (event.shiftKey && document.activeElement === first) {
      event.preventDefault()
      last?.focus()
    } else if (!event.shiftKey && document.activeElement === last) {
      event.preventDefault()
      first?.focus()
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
