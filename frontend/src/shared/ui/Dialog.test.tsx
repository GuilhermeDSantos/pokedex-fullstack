import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it } from 'vitest'
import { Dialog } from './Dialog'

function renderDialog() {
  render(
    <>
      <button type="button">Behind the dialog</button>
      <Dialog title="Edit our fields" onClose={() => {}}>
        <input aria-label="Localized name" />
        <button type="button">Save</button>
      </Dialog>
    </>,
  )
}

describe('Dialog', () => {
  // A modal keeps the keyboard inside: Tab never reaches the page behind it.
  it('keeps focus inside, wrapping from the last control to the first and back', async () => {
    renderDialog()
    const first = screen.getByRole('textbox', { name: 'Localized name' })
    const last = screen.getByRole('button', { name: 'Save' })
    expect(first).toHaveFocus()

    await userEvent.tab()
    expect(last).toHaveFocus()
    await userEvent.tab()
    expect(first).toHaveFocus()
    await userEvent.tab({ shift: true })
    expect(last).toHaveFocus()
  })
})
