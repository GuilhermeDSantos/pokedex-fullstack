import { http, HttpResponse } from 'msw'
import { describe, expect, it } from 'vitest'
import { server } from '../../test/msw/server'
import { request } from './httpClient'

describe('httpClient', () => {
  it('calls the API under /api/v1 and returns the parsed body', async () => {
    server.use(http.get('/api/v1/auth/me', () => HttpResponse.json({ name: 'Ash Ketchum' })))

    await expect(request('/auth/me')).resolves.toEqual({ name: 'Ash Ketchum' })
  })
})
