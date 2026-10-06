import { render, screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import { Button } from './Button'

describe('Button', () => {
  it('is disabled while its action is pending, so it cannot be sent twice', () => {
    render(
      <Button type="submit" pending>
        Sign in
      </Button>,
    )

    expect(screen.getByRole('button', { name: 'Sign in' })).toBeDisabled()
  })

  it('does not submit a surrounding form unless it is a submit button', () => {
    render(<Button>Retry</Button>)

    expect(screen.getByRole('button', { name: 'Retry' })).toHaveAttribute('type', 'button')
  })
})
