import { screen, waitForElementToBeRemoved, within } from '@testing-library/react'
import { http, HttpResponse } from 'msw'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'
import { BULBASAUR, PIKACHU, pageOf } from '../../../test/fixtures/pokemon'
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

  it('explains when the catalog is unavailable and loads the page on retry', async () => {
    const unavailable = 'The service is temporarily unavailable. Please try again in a moment.'
    server.use(
      http.get(
        '/api/v1/pokemon',
        () => HttpResponse.json({ code: 'DATA_UNAVAILABLE', message: unavailable, fieldErrors: [] }, { status: 503 }),
        { once: true },
      ),
    )
    renderApp('/')

    expect(await screen.findByRole('alert')).toHaveTextContent(unavailable)
    await userEvent.click(screen.getByRole('button', { name: 'Retry' }))
    expect(await screen.findByRole('article', { name: 'Bulbasaur' })).toBeInTheDocument()
  })

  it('says so when a page has no Pokémon, with a way back to the first page', async () => {
    server.use(http.get('/api/v1/pokemon', () => HttpResponse.json(pageOf([], { page: 998 }))))
    renderApp('/')

    expect(await screen.findByText('No Pokémon on this page.')).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Go to the first page' })).toHaveAttribute('href', '/')
  })

  // The URL counts pages from 1, as people do; the API counts from 0.
  it('shows the page the URL asks for', async () => {
    server.use(
      http.get('/api/v1/pokemon', ({ request }) => {
        const page = Number(new URL(request.url).searchParams.get('page'))
        return HttpResponse.json(pageOf(page === 2 ? [PIKACHU] : [BULBASAUR], { page }))
      }),
    )
    renderApp('/?page=3')

    expect(await screen.findByRole('article', { name: 'Pikachu' })).toBeInTheDocument()
    expect(within(screen.getByRole('navigation', { name: 'Pagination' })).getByText('Page 3 of 68')).toBeInTheDocument()
  })

  it('moves between pages through links, so each page has its own URL', async () => {
    const { router } = renderApp('/?page=3')

    const pagination = await screen.findByRole('navigation', { name: 'Pagination' })
    expect(within(pagination).getByRole('link', { name: 'Previous page' })).toHaveAttribute('href', '/?page=2')
    await userEvent.click(within(pagination).getByRole('link', { name: 'Next page' }))

    expect(router.state.location.search).toBe('?page=4')
    expect(await screen.findByText('Page 4 of 68')).toBeInTheDocument()
  })

  it.each(['abc', '0', '-2', '1.5'])('treats ?page=%s as the first page', async (page) => {
    server.use(
      http.get('/api/v1/pokemon', ({ request }) =>
        new URL(request.url).searchParams.get('page') === '0'
          ? HttpResponse.json(pageOf([BULBASAUR]))
          : HttpResponse.json({ code: 'VALIDATION_ERROR', message: 'Invalid request parameter', fieldErrors: [] }, { status: 400 }),
      ),
    )
    renderApp(`/?page=${page}`)

    expect(await screen.findByRole('article', { name: 'Bulbasaur' })).toBeInTheDocument()
  })

  // Pagination sits below the grid, so without this the next page would open scrolled to its bottom.
  it('returns to the top when moving to another page', async () => {
    renderApp('/?page=3')

    const pagination = await screen.findByRole('navigation', { name: 'Pagination' })
    vi.mocked(window.scrollTo).mockClear()
    await userEvent.click(within(pagination).getByRole('link', { name: 'Next page' }))

    expect(window.scrollTo).toHaveBeenCalledWith(0, 0)
  })

  it("opens a Pokémon's detail from its card", async () => {
    server.use(http.get('/api/v1/pokemon', () => HttpResponse.json(pageOf([PIKACHU]))))
    const { router } = renderApp('/')

    const card = await screen.findByRole('article', { name: 'Pikachu' })
    await userEvent.click(within(card).getByRole('link', { name: 'Pikachu' }))

    expect(router.state.location.pathname).toBe('/pokemon/pikachu')
  })
})
