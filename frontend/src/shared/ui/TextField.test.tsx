import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { useState } from 'react'
import { describe, expect, it } from 'vitest'
import { TextField } from './TextField'

function ControlledEmail({ error }: { error?: string }) {
  const [email, setEmail] = useState('')
  return <TextField label="Email" name="email" type="email" value={email} onChange={setEmail} error={error} />
}

describe('TextField', () => {
  it('is reachable by its label and reports what the user types', async () => {
    render(<ControlledEmail />)

    await userEvent.type(screen.getByLabelText('Email'), 'ash@pallet.town')

    expect(screen.getByLabelText('Email')).toHaveValue('ash@pallet.town')
  })

  it('marks the input invalid and describes it with the error', () => {
    render(<ControlledEmail error="Email must be a valid address" />)

    const input = screen.getByLabelText('Email')
    expect(input).toHaveAttribute('aria-invalid', 'true')
    expect(input).toHaveAccessibleDescription('Email must be a valid address')
  })

  it('is not marked invalid without an error', () => {
    render(<ControlledEmail />)

    expect(screen.getByLabelText('Email')).not.toHaveAttribute('aria-invalid')
  })

  it('describes the input with its hint, so the rule is known before typing', () => {
    render(<TextField label="Password" name="password" value="" onChange={() => {}} hint="At least 8 characters" />)

    expect(screen.getByLabelText('Password')).toHaveAccessibleDescription('At least 8 characters')
  })

  it('announces the error before the hint when there are both', () => {
    render(
      <TextField
        label="Password"
        name="password"
        value=""
        onChange={() => {}}
        hint="At least 8 characters"
        error="Password is required"
      />,
    )

    expect(screen.getByLabelText('Password')).toHaveAccessibleDescription('Password is required At least 8 characters')
  })
})
