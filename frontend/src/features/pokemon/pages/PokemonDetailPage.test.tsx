import { screen, waitForElementToBeRemoved, within } from '@testing-library/react'
import { http, HttpResponse } from 'msw'
import userEvent from '@testing-library/user-event'
import { describe, expect, it } from 'vitest'
import { PIKACHU_DETAIL, SYNCED_PIKACHU_DETAIL } from '../../../test/fixtures/pokemon'
import { ASH_SESSION } from '../../../test/fixtures/session'
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

  it('says so when PokeAPI has no such Pokémon, with a way back to the list', async () => {
    server.use(
      http.get('/api/v1/pokemon/missingno', () =>
        HttpResponse.json(
          { code: 'NOT_FOUND', message: "Pokémon 'missingno' was not found", fieldErrors: [] },
          { status: 404 },
        ),
      ),
    )
    renderApp('/pokemon/missingno')

    expect(await screen.findByRole('heading', { level: 1, name: 'Pokémon not found' })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Back to the Pokémon list' })).toHaveAttribute('href', '/')
  })

  it('explains when the catalog is unavailable and loads the Pokémon on retry', async () => {
    const unavailable = 'The service is temporarily unavailable. Please try again in a moment.'
    server.use(
      http.get(
        '/api/v1/pokemon/pikachu',
        () => HttpResponse.json({ code: 'DATA_UNAVAILABLE', message: unavailable, fieldErrors: [] }, { status: 503 }),
        { once: true },
      ),
    )
    renderApp('/pokemon/pikachu')

    expect(await screen.findByRole('alert')).toHaveTextContent(unavailable)
    await userEvent.click(screen.getByRole('button', { name: 'Retry' }))
    expect(await screen.findByRole('heading', { level: 1, name: 'Pikachu' })).toBeInTheDocument()
  })

  it('shows a placeholder while the Pokémon loads', async () => {
    renderApp('/pokemon/pikachu')

    await waitForElementToBeRemoved(screen.getByRole('status', { name: 'Loading Pokémon' }))
    expect(screen.getByRole('heading', { level: 1, name: 'Pikachu' })).toBeInTheDocument()
  })

  it('shows the category, height, weight, types and abilities', async () => {
    renderApp('/pokemon/pikachu')

    const facts = await screen.findByRole('list', { name: 'Facts' })
    expect(facts).toHaveTextContent('CategoryMouse Pokémon')
    expect(facts).toHaveTextContent('Height0.4 m')
    expect(facts).toHaveTextContent('Weight6.0 kg')
    expect(screen.getByRole('list', { name: 'Types' })).toHaveTextContent('Electric')
    expect(screen.getByRole('list', { name: 'Abilities' })).toHaveTextContent('StaticLightning Rod (hidden)')
  })

  it('falls back to the sprite without artwork, and to a placeholder without either', async () => {
    server.use(
      http.get('/api/v1/pokemon/pikachu', () => HttpResponse.json({ ...PIKACHU_DETAIL, artworkUrl: null })),
      http.get('/api/v1/pokemon/raichu', () =>
        HttpResponse.json({ ...PIKACHU_DETAIL, name: 'raichu', artworkUrl: null, spriteUrl: null }),
      ),
    )
    const { router } = renderApp('/pokemon/pikachu')

    expect(await screen.findByRole('img', { name: 'Pikachu sprite' })).toHaveAttribute('src', 'https://img.test/25.png')
    await router.navigate('/pokemon/raichu')
    expect(await screen.findByText('No image')).toBeInTheDocument()
  })

  // ---- the local record (US-03) --------------------------------------------------------------

  it('invites a visitor to sign in to sync, coming back to this Pokémon afterwards', async () => {
    renderApp('/pokemon/pikachu')

    const local = await screen.findByRole('region', { name: 'Local data' })
    expect(within(local).getByRole('link', { name: 'Log in to sync' })).toHaveAttribute(
      'href',
      '/login?returnTo=%2Fpokemon%2Fpikachu',
    )
  })

  it('syncs the Pokémon for a signed-in user and then shows our record', async () => {
    let synced = false
    server.use(
      http.get('/api/v1/pokemon/pikachu', () => HttpResponse.json(synced ? SYNCED_PIKACHU_DETAIL : PIKACHU_DETAIL)),
      http.post('/api/v1/pokemon/pikachu/local', ({ request }) => {
        if (request.headers.get('Authorization') !== `Bearer ${ASH_SESSION.accessToken}`) {
          return HttpResponse.json({ code: 'UNAUTHENTICATED', message: 'No token', fieldErrors: [] }, { status: 401 })
        }
        synced = true
        return HttpResponse.json(SYNCED_PIKACHU_DETAIL.local, { status: 201 })
      }),
    )
    renderApp('/pokemon/pikachu', { session: ASH_SESSION })

    await userEvent.click(await screen.findByRole('button', { name: 'Sync to local database' }))

    const local = screen.getByRole('region', { name: 'Local data' })
    expect(await within(local).findByText('Kanto')).toBeInTheDocument()
    expect(within(local).queryByRole('button', { name: 'Sync to local database' })).not.toBeInTheDocument()
  })

  // Someone else synced it first: not an error for this user, just show the record that now exists.
  it('shows the existing record with a note when the Pokémon was synced meanwhile', async () => {
    let syncedElsewhere = false
    server.use(
      http.get('/api/v1/pokemon/pikachu', () =>
        HttpResponse.json(syncedElsewhere ? SYNCED_PIKACHU_DETAIL : PIKACHU_DETAIL),
      ),
      http.post('/api/v1/pokemon/pikachu/local', () => {
        syncedElsewhere = true
        return HttpResponse.json(
          { code: 'CONFLICT', message: 'Pokémon #25 is already in the local database', fieldErrors: [] },
          { status: 409 },
        )
      }),
    )
    renderApp('/pokemon/pikachu', { session: ASH_SESSION })

    await userEvent.click(await screen.findByRole('button', { name: 'Sync to local database' }))

    const local = screen.getByRole('region', { name: 'Local data' })
    expect(await within(local).findByText('Kanto')).toBeInTheDocument()
    expect(within(local).getByRole('status')).toHaveTextContent('Someone synced this Pokémon just before you.')
  })
})
