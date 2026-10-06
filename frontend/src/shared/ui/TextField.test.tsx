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
})
