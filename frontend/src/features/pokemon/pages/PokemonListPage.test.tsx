import { screen, waitForElementToBeRemoved, within } from '@testing-library/react'
import { http, HttpResponse } from 'msw'
import { describe, expect, it } from 'vitest'
import { PIKACHU, pageOf } from '../../../test/fixtures/pokemon'
import { server } from '../../../test/msw/server'
import { renderApp } from '../../../test/renderApp'

describe('PokemonListPage', () => {
  it("shows each Pokémon's sprite, category, mass and skills, with its name and number", async () => {
    server.use(http.get('/api/v1/pokemon', () => HttpResponse.json(pageOf([PIKACHU]))))
    renderApp('/')

    const card = await screen.findByRole('article', { name: 'Pikachu' })
    expect(within(card).getByRole('img', { name: 'Pikachu sprite' })).toHaveAttribute('src', 'https://img.test/25.png')
    expect(within(card).getByText('#025')).toBeInTheDocument()
    expect(within(card).getByText('Mouse Pokémon')).toBeInTheDocument()
    expect(within(card).getByText('6.0 kg')).toBeInTheDocument()
    expect(within(card).getByRole('list', { name: 'Abilities' })).toHaveTextContent('StaticLightning Rod (hidden)')
    expect(within(card).getByRole('list', { name: 'Types' })).toHaveTextContent('Electric')
  })

  it('shows placeholders while the page loads, then the cards', async () => {
    renderApp('/')

    const loading = screen.getByRole('status', { name: 'Loading Pokémon' })
    await waitForElementToBeRemoved(loading)
    expect(screen.getByRole('article', { name: 'Bulbasaur' })).toBeInTheDocument()
  })
})
