import { screen, waitFor, waitForElementToBeRemoved, within } from '@testing-library/react'
import { http, HttpResponse } from 'msw'
import userEvent from '@testing-library/user-event'
import { describe, expect, it } from 'vitest'
import { EDITED_PIKACHU_DETAIL, PIKACHU_DETAIL, SYNCED_PIKACHU_DETAIL } from '../../../test/fixtures/pokemon'
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

    expect(await screen.findByRole('link', { name: 'Log in to sync' })).toHaveAttribute(
      'href',
      '/login?returnTo=%2Fpokemon%2Fpikachu',
    )
  })

  it('syncs the Pokémon for a signed-in user and then shows our record', async () => {
    let synced = false
    server.use(
      http.get('/api/v1/pokemon/pikachu', () => HttpResponse.json(synced ? SYNCED_PIKACHU_DETAIL : PIKACHU_DETAIL)),
      http.post('/api/v1/pokemon/25/local', ({ request }) => {
        if (request.headers.get('Authorization') !== `Bearer ${ASH_SESSION.accessToken}`) {
          return HttpResponse.json({ code: 'UNAUTHENTICATED', message: 'No token', fieldErrors: [] }, { status: 401 })
        }
        synced = true
        return HttpResponse.json(SYNCED_PIKACHU_DETAIL.local, { status: 201 })
      }),
    )
    renderApp('/pokemon/pikachu', { session: ASH_SESSION })

    await userEvent.click(await screen.findByRole('button', { name: 'Sync to local database' }))

    expect(await screen.findByText('Kanto')).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Sync to local database' })).not.toBeInTheDocument()
  })

  // The same Pokémon can be open under its number: the page still shows the new record.
  it('shows our record after a sync from a page opened by Pokédex number', async () => {
    let synced = false
    server.use(
      http.get('/api/v1/pokemon/25', () => HttpResponse.json(synced ? SYNCED_PIKACHU_DETAIL : PIKACHU_DETAIL)),
      http.post('/api/v1/pokemon/25/local', () => {
        synced = true
        return HttpResponse.json(SYNCED_PIKACHU_DETAIL.local, { status: 201 })
      }),
    )
    renderApp('/pokemon/25', { session: ASH_SESSION })

    await userEvent.click(await screen.findByRole('button', { name: 'Sync to local database' }))

    expect(await screen.findByText('Kanto')).toBeInTheDocument()
  })

  // Someone else synced it first: not an error for this user, just show the record that now exists.
  it('shows the existing record with a note when the Pokémon was synced meanwhile', async () => {
    let syncedElsewhere = false
    server.use(
      http.get('/api/v1/pokemon/pikachu', () =>
        HttpResponse.json(syncedElsewhere ? SYNCED_PIKACHU_DETAIL : PIKACHU_DETAIL),
      ),
      http.post('/api/v1/pokemon/25/local', () => {
        syncedElsewhere = true
        return HttpResponse.json(
          { code: 'CONFLICT', message: 'Pokémon #25 is already in the local database', fieldErrors: [] },
          { status: 409 },
        )
      }),
    )
    renderApp('/pokemon/pikachu', { session: ASH_SESSION })

    await userEvent.click(await screen.findByRole('button', { name: 'Sync to local database' }))

    expect(await screen.findByText('Kanto')).toBeInTheDocument()
    expect(screen.getByRole('status')).toHaveTextContent('Someone synced this Pokémon just before you.')
  })

  // The token expired, or the account is gone (D-033): the session is over, so sign in again and come back.
  it('signs the user out and sends them to sign in when the sync is refused as unauthenticated', async () => {
    server.use(
      http.post('/api/v1/pokemon/25/local', () =>
        HttpResponse.json(
          { code: 'UNAUTHENTICATED', message: 'Authentication is required to access this resource', fieldErrors: [] },
          { status: 401 },
        ),
      ),
    )
    const { router } = renderApp('/pokemon/pikachu', { session: ASH_SESSION })

    await userEvent.click(await screen.findByRole('button', { name: 'Sync to local database' }))

    await waitFor(() => expect(router.state.location.pathname).toBe('/login'))
    expect(router.state.location.search).toBe('?returnTo=%2Fpokemon%2Fpikachu')
    expect(within(screen.getByRole('banner')).getByRole('link', { name: 'Sign in' })).toBeInTheDocument()
    expect(sessionStorage.length).toBe(0)
  })

  // One Pokémon on screen: our fields sit with PokeAPI's, the reader never sees two sources.
  it('shows our region among the facts and our tags as a list, like the types', async () => {
    server.use(http.get('/api/v1/pokemon/pikachu', () => HttpResponse.json(SYNCED_PIKACHU_DETAIL)))
    renderApp('/pokemon/pikachu')

    const facts = await screen.findByRole('list', { name: 'Facts' })
    expect(within(facts).getByText('Region').parentElement).toHaveTextContent('Kanto')
    const tags = screen.getByRole('list', { name: 'Tags' })
    expect(within(tags).getAllByRole('listitem').map((tag) => tag.textContent)).toEqual(['Mascot', 'Starter'])
    expect(screen.queryByRole('region', { name: 'Local data' })).not.toBeInTheDocument()
  })

  // A nickname: shown as "Pica", and it is still Pikachu underneath (D-039).
  it('keeps the name as the title and shows the localized name right under it', async () => {
    server.use(http.get('/api/v1/pokemon/pikachu', () => HttpResponse.json(SYNCED_PIKACHU_DETAIL)))
    renderApp('/pokemon/pikachu')

    const title = await screen.findByRole('heading', { level: 1, name: 'Pikachu' })
    expect(title.nextElementSibling).toHaveTextContent('Pica')
  })

  // ---- editing and removing our record (US-04, CRUD-D) -----------------------------------------

  it('lets a signed-in user edit our fields and then shows the saved values', async () => {
    let detail = SYNCED_PIKACHU_DETAIL
    let sent: unknown = null
    server.use(
      http.get('/api/v1/pokemon/pikachu', () => HttpResponse.json(detail)),
      http.put('/api/v1/pokemon/25/local', async ({ request }) => {
        sent = await request.json()
        detail = EDITED_PIKACHU_DETAIL
        return HttpResponse.json({ pokedexNumber: 25, ...EDITED_PIKACHU_DETAIL.local })
      }),
    )
    renderApp('/pokemon/pikachu', { session: ASH_SESSION })

    await userEvent.click(await screen.findByRole('button', { name: 'Edit' }))
    const localizedName = screen.getByRole('textbox', { name: 'Localized name' })
    const region = screen.getByRole('textbox', { name: 'Region' })
    const tags = screen.getByRole('textbox', { name: 'Tags' })
    expect(localizedName).toHaveValue('Pica')
    expect(tags).toHaveValue('mascot, starter')
    await userEvent.clear(localizedName)
    await userEvent.type(localizedName, 'Pikachu BR')
    await userEvent.clear(region)
    await userEvent.type(region, 'Johto')
    await userEvent.clear(tags)
    await userEvent.type(tags, 'electric')
    await userEvent.click(screen.getByRole('button', { name: 'Save' }))

    expect(await screen.findByText('Johto')).toBeInTheDocument()
    expect(sent).toEqual({ localizedName: 'Pikachu BR', region: 'Johto', tags: ['electric'] })
    expect(screen.queryByRole('button', { name: 'Save' })).not.toBeInTheDocument()
  })

  it('shows what the API rejected next to the field, keeping what the user typed', async () => {
    server.use(
      http.get('/api/v1/pokemon/pikachu', () => HttpResponse.json(SYNCED_PIKACHU_DETAIL)),
      http.put('/api/v1/pokemon/25/local', () =>
        HttpResponse.json(
          {
            code: 'VALIDATION_ERROR',
            message: 'Request body is invalid',
            fieldErrors: [{ field: 'region', message: 'size must be between 0 and 100' }],
          },
          { status: 400 },
        ),
      ),
    )
    renderApp('/pokemon/pikachu', { session: ASH_SESSION })

    await userEvent.click(await screen.findByRole('button', { name: 'Edit' }))
    const region = screen.getByRole('textbox', { name: 'Region' })
    await userEvent.type(region, ' and beyond')
    await userEvent.click(screen.getByRole('button', { name: 'Save' }))

    expect(await screen.findByText('size must be between 0 and 100')).toBeInTheDocument()
    expect(region).toHaveAttribute('aria-invalid', 'true')
    expect(region).toHaveValue('Kanto and beyond')
  })
})
