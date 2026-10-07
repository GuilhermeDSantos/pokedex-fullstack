import { render, screen } from '@testing-library/react'
import { createMemoryRouter } from 'react-router'
import { RouterProvider } from 'react-router/dom'
import { describe, expect, it } from 'vitest'
import type { PokemonSummary } from '../api/pokemonApi'
import { PIKACHU } from '../../../test/fixtures/pokemon'
import { PokemonCard } from './PokemonCard'

// Inside a router, as on the page: a card may link to the Pokémon's detail.
function renderCard(pokemon: PokemonSummary) {
  render(<RouterProvider router={createMemoryRouter([{ path: '/', element: <PokemonCard pokemon={pokemon} /> }])} />)
}

describe('PokemonCard', () => {
  it('shows a placeholder instead of a broken image when PokeAPI has no sprite', () => {
    renderCard({ ...PIKACHU, spriteUrl: null })

    expect(screen.queryByRole('img')).not.toBeInTheDocument()
    expect(screen.getByText('No image')).toBeInTheDocument()
  })

  it('says the category is unknown when PokeAPI has no English one', () => {
    renderCard({ ...PIKACHU, category: null })

    expect(screen.getByText('Category unknown')).toBeInTheDocument()
  })

  // As on the detail: the name is the title, our localized name sits right under it.
  it('shows our localized name under the name when there is one', () => {
    renderCard({ ...PIKACHU, localizedName: 'Pica' })

    expect(screen.getByRole('heading', { name: 'Pikachu' }).nextElementSibling).toHaveTextContent('Pica')
  })

  it('shows no second name when there is no localized one', () => {
    renderCard(PIKACHU)

    expect(screen.getByRole('heading', { name: 'Pikachu' }).nextElementSibling).toHaveTextContent('Mouse Pokémon')
  })
})
