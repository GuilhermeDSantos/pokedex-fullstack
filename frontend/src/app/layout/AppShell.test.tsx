import { screen, within } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import { renderApp } from '../../test/renderApp'

describe('header session area', () => {
  it('offers to sign in or create an account when nobody is signed in', () => {
    renderApp('/')

    const banner = screen.getByRole('banner')
    expect(within(banner).getByRole('link', { name: 'Sign in' })).toHaveAttribute('href', '/login')
    expect(within(banner).getByRole('link', { name: 'Create account' })).toHaveAttribute('href', '/register')
  })
})
