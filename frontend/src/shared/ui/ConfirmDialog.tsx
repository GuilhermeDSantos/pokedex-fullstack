import { Button } from './Button'
import { Dialog } from './Dialog'
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
  return (
    <Dialog title={title} description={message} role="alertdialog" onClose={onCancel}>
      <div className={styles.actions}>
        <Button onClick={onCancel}>Cancel</Button>
        <Button pending={pending} onClick={onConfirm}>
          {confirmLabel}
        </Button>
      </div>
    </Dialog>
  )
}
