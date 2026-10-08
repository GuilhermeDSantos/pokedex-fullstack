import { screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it } from 'vitest'
import { ASH_SESSION } from '../../test/fixtures/session'
import { renderApp } from '../../test/renderApp'

describe('header session area', () => {
  // Signing in from a page comes back to that page, list page and its query included.
  it('offers to sign in or create an account when nobody is signed in, coming back to the page', () => {
    renderApp('/?page=2')

    const banner = screen.getByRole('banner')
    expect(within(banner).getByRole('link', { name: 'Sign in' })).toHaveAttribute('href', '/login?returnTo=%2F%3Fpage%3D2')
    expect(within(banner).getByRole('link', { name: 'Create account' })).toHaveAttribute(
      'href',
      '/register?returnTo=%2F%3Fpage%3D2',
    )
  })

  it('shows who is signed in after a reload, with a way to sign out', () => {
    renderApp('/', { session: ASH_SESSION })

    const banner = screen.getByRole('banner')
    expect(within(banner).getByText('Ash Ketchum')).toBeInTheDocument()
    expect(within(banner).getByRole('button', { name: 'Sign out' })).toBeInTheDocument()
    expect(within(banner).queryByRole('link', { name: 'Sign in' })).not.toBeInTheDocument()
  })

  it('drops a stored session whose token has expired', () => {
    renderApp('/', { session: { ...ASH_SESSION, expiresAt: '2000-01-01T00:00:00Z' } })

    expect(within(screen.getByRole('banner')).getByRole('link', { name: 'Sign in' })).toBeInTheDocument()
    expect(sessionStorage.length).toBe(0)
  })

  it('signs out, forgetting the session for this tab too', async () => {
    renderApp('/', { session: ASH_SESSION })

    await userEvent.click(screen.getByRole('button', { name: 'Sign out' }))

    expect(within(screen.getByRole('banner')).getByRole('link', { name: 'Sign in' })).toBeInTheDocument()
    expect(sessionStorage.length).toBe(0)
  })
})
