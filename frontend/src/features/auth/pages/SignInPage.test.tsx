import { screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { http, HttpResponse } from 'msw'
import { describe, expect, it } from 'vitest'
import { ASH, ASH_SESSION } from '../../../test/fixtures/session'
import { server } from '../../../test/msw/server'
import { renderApp } from '../../../test/renderApp'

async function signIn(email: string, password: string) {
  await userEvent.type(screen.getByLabelText('Email'), email)
  await userEvent.type(screen.getByLabelText('Password'), password)
  await userEvent.click(screen.getByRole('button', { name: 'Sign in' }))
}

describe('SignInPage', () => {
  it('signs the user in and returns to where they came from', async () => {
    server.use(
      http.post('/api/v1/auth/login', () =>
        HttpResponse.json({ accessToken: ASH_SESSION.accessToken, tokenType: 'Bearer', expiresAt: ASH_SESSION.expiresAt }),
      ),
      http.get('/api/v1/auth/me', ({ request }) =>
        request.headers.get('Authorization') === `Bearer ${ASH_SESSION.accessToken}`
          ? HttpResponse.json(ASH)
          : HttpResponse.json({ code: 'UNAUTHENTICATED', message: 'No token', fieldErrors: [] }, { status: 401 }),
      ),
    )
    const { router } = renderApp('/login?returnTo=/pokemon/25')

    await signIn('ash@pallet.town', 'pikachu123')

    await waitFor(() => expect(router.state.location.pathname).toBe('/pokemon/25'))
    expect(within(screen.getByRole('banner')).getByText('Ash Ketchum')).toBeInTheDocument()
    expect(sessionStorage.length).toBe(1)
  })

  it('shows invalid credentials as a form error and stays on the page', async () => {
    server.use(
      http.post('/api/v1/auth/login', () =>
        HttpResponse.json(
          { code: 'UNAUTHENTICATED', message: 'Invalid email or password', fieldErrors: [] },
          { status: 401 },
        ),
      ),
    )
    const { router } = renderApp('/login')

    await signIn('ash@pallet.town', 'charmander1')

    expect(await screen.findByRole('alert')).toHaveTextContent('Invalid email or password')
    expect(router.state.location.pathname).toBe('/login')
    expect(within(screen.getByRole('banner')).getByRole('link', { name: 'Sign in' })).toBeInTheDocument()
  })

  it('asks for the missing fields before calling the server', async () => {
    renderApp('/login')

    await userEvent.click(screen.getByRole('button', { name: 'Sign in' }))

    expect(screen.getByLabelText('Email')).toHaveAccessibleDescription('Email is required')
    expect(screen.getByLabelText('Password')).toHaveAccessibleDescription('Password is required')
  })
})
