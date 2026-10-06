import { http, HttpResponse } from 'msw'
import { describe, expect, it } from 'vitest'
import { server } from '../../test/msw/server'
import { request } from './httpClient'

describe('httpClient', () => {
  it('calls the API under /api/v1 and returns the parsed body', async () => {
    server.use(http.get('/api/v1/auth/me', () => HttpResponse.json({ name: 'Ash Ketchum' })))

    await expect(request('/auth/me')).resolves.toEqual({ name: 'Ash Ketchum' })
  })

  it('sends a body as JSON', async () => {
    server.use(
      http.post('/api/v1/auth/login', async ({ request: received }) => {
        if (received.headers.get('Content-Type') !== 'application/json') {
          return HttpResponse.json({ code: 'WRONG_CONTENT_TYPE' }, { status: 415 })
        }
        return HttpResponse.json({ received: await received.json() })
      }),
    )

    await expect(
      request('/auth/login', { method: 'POST', body: { email: 'ash@pallet.town', password: 'pikachu123' } }),
    ).resolves.toEqual({ received: { email: 'ash@pallet.town', password: 'pikachu123' } })
  })
})
