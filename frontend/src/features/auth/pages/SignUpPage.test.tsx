import { screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { http, HttpResponse } from 'msw'
import { describe, expect, it } from 'vitest'
import { ASH, ASH_SESSION } from '../../../test/fixtures/session'
import { server } from '../../../test/msw/server'
import { renderApp } from '../../../test/renderApp'

async function signUp(name: string, email: string, password: string) {
  await userEvent.type(screen.getByLabelText('Name'), name)
  await userEvent.type(screen.getByLabelText('Email'), email)
  await userEvent.type(screen.getByLabelText('Password'), password)
  await userEvent.click(screen.getByRole('button', { name: 'Create account' }))
}

describe('SignUpPage', () => {
  it('creates the account, signs the user in straight away and returns them', async () => {
    server.use(
      http.post('/api/v1/auth/register', () => HttpResponse.json(ASH, { status: 201 })),
      http.post('/api/v1/auth/login', () =>
        HttpResponse.json({ accessToken: ASH_SESSION.accessToken, tokenType: 'Bearer', expiresAt: ASH_SESSION.expiresAt }),
      ),
      http.get('/api/v1/auth/me', () => HttpResponse.json(ASH)),
    )
    const { router } = renderApp('/register?returnTo=/pokemon/25')

    await signUp('Ash Ketchum', 'ash@pallet.town', 'pikachu123')

    await waitFor(() => expect(router.state.location.pathname).toBe('/pokemon/25'))
    expect(within(screen.getByRole('banner')).getByText('Ash Ketchum')).toBeInTheDocument()
  })
})
