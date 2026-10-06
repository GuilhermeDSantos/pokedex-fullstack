import { render, screen, within } from '@testing-library/react'
import { createMemoryRouter } from 'react-router'
import { RouterProvider } from 'react-router/dom'
import { describe, expect, it } from 'vitest'
import { routes } from './router'

function renderAt(path: string) {
  render(<RouterProvider router={createMemoryRouter(routes, { initialEntries: [path] })} />)
}

describe('app shell', () => {
  it('shows a header that links back to the start page, and a main region', () => {
    renderAt('/')

    const banner = screen.getByRole('banner')
    expect(within(banner).getByRole('link', { name: 'Pokémon Catalog' })).toHaveAttribute('href', '/')
    expect(screen.getByRole('main')).toBeInTheDocument()
  })
})
