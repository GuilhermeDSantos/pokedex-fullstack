import { screen, within } from '@testing-library/react'
import { http, HttpResponse } from 'msw'
import { describe, expect, it } from 'vitest'
import { PIKACHU_DETAIL } from '../../../test/fixtures/pokemon'
import { server } from '../../../test/msw/server'
import { renderApp } from '../../../test/renderApp'

describe('PokemonDetailPage', () => {
  it('shows the image, core statistics and description of the chosen Pokémon', async () => {
    server.use(http.get('/api/v1/pokemon/pikachu', () => HttpResponse.json(PIKACHU_DETAIL)))
    renderApp('/pokemon/pikachu')

    expect(await screen.findByRole('heading', { level: 1, name: 'Pikachu' })).toBeInTheDocument()
    expect(screen.getByRole('img', { name: 'Pikachu artwork' })).toHaveAttribute('src', 'https://img.test/25-art.png')
    expect(screen.getByText('#025')).toBeInTheDocument()
    expect(screen.getByText('Possesses cheek sacs in which it stores electricity.')).toBeInTheDocument()
    const stats = screen.getByRole('list', { name: 'Base stats' })
    expect(within(stats).getAllByRole('listitem').map((item) => item.textContent)).toEqual([
      'HP35',
      'Attack55',
      'Defense40',
      'Sp. Atk50',
      'Sp. Def50',
      'Speed90',
    ])
  })

  it('shows the whole lineage, every branch included, with the current Pokémon marked', async () => {
    const eeveelutions = ['vaporeon', 'jolteon', 'flareon', 'espeon', 'umbreon', 'leafeon', 'glaceon', 'sylveon']
    server.use(
      http.get('/api/v1/pokemon/eevee', () =>
        HttpResponse.json({
          ...PIKACHU_DETAIL,
          pokedexNumber: 133,
          name: 'eevee',
          evolutionChain: {
            speciesName: 'eevee',
            pokedexNumber: 133,
            evolvesTo: eeveelutions.map((speciesName, branch) => ({
              speciesName,
              pokedexNumber: 134 + branch,
              evolvesTo: [],
            })),
          },
        }),
      ),
    )
    renderApp('/pokemon/eevee')

    const lineage = await screen.findByRole('navigation', { name: 'Evolution' })
    expect(within(lineage).getByRole('link', { name: 'Eevee' })).toHaveAttribute('aria-current', 'page')
    expect(within(lineage).getByRole('link', { name: 'Sylveon' })).toHaveAttribute('href', '/pokemon/sylveon')
    expect(within(lineage).getAllByRole('link')).toHaveLength(9)
  })
})
