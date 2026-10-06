import { screen, within } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import { renderApp } from '../test/renderApp'

describe('app shell', () => {
  it('shows a header that links back to the start page, and a main region', () => {
    renderApp('/')

    const banner = screen.getByRole('banner')
    expect(within(banner).getByRole('link', { name: 'Pokémon Catalog' })).toHaveAttribute('href', '/')
    expect(screen.getByRole('main')).toBeInTheDocument()
  })

  it('answers an unknown path with a not-found page inside the shell', () => {
    renderApp('/no-such-page')

    expect(screen.getByRole('heading', { level: 1, name: 'Page not found' })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Back to the start page' })).toHaveAttribute('href', '/')
    expect(screen.getByRole('banner')).toBeInTheDocument()
  })
})
