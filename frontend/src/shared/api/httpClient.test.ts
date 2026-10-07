import { http, HttpResponse } from 'msw'
import { describe, expect, it } from 'vitest'
import { server } from '../../test/msw/server'
import { ApiError } from './ApiError'
import { request } from './httpClient'

describe('httpClient', () => {
  it('calls the API under /api/v1 and returns the parsed body', async () => {
    server.use(http.get('/api/v1/auth/me', () => HttpResponse.json({ name: 'Ash Ketchum' })))

    await expect(request('/auth/me')).resolves.toEqual({ name: 'Ash Ketchum' })
  })

  // A DELETE answers 204 with no body: there is nothing to parse.
  it('resolves with nothing when the response has no content', async () => {
    server.use(http.delete('/api/v1/pokemon/25/local', () => new HttpResponse(null, { status: 204 })))

    await expect(request('/pokemon/25/local', { method: 'DELETE' })).resolves.toBeUndefined()
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

  it('sends the access token as a bearer token when the call is given one', async () => {
    server.use(
      http.get('/api/v1/auth/me', ({ request: received }) =>
        HttpResponse.json({ authorization: received.headers.get('Authorization') }),
      ),
    )

    await expect(request('/auth/me', { accessToken: 'signed.jwt.value' })).resolves.toEqual({
      authorization: 'Bearer signed.jwt.value',
    })
  })

  it('sends no Authorization header when the call is given no token', async () => {
    server.use(
      http.get('/api/v1/pokemon', ({ request: received }) =>
        HttpResponse.json({ hasAuthorization: received.headers.has('Authorization') }),
      ),
    )

    await expect(request('/pokemon')).resolves.toEqual({ hasAuthorization: false })
  })

  it('turns an ErrorResponse into an ApiError the UI can branch on', async () => {
    server.use(
      http.post('/api/v1/auth/register', () =>
        HttpResponse.json(
          {
            code: 'VALIDATION_ERROR',
            message: 'Request body is invalid',
            fieldErrors: [{ field: 'email', message: 'must not be blank' }],
          },
          { status: 400 },
        ),
      ),
    )

    const error = await request('/auth/register', { method: 'POST', body: {} }).catch((caught: unknown) => caught)

    expect(error).toBeInstanceOf(ApiError)
    expect(error).toMatchObject({
      status: 400,
      code: 'VALIDATION_ERROR',
      message: 'Request body is invalid',
      fieldErrors: [{ field: 'email', message: 'must not be blank' }],
    })
  })

  it('still gives an ApiError when an error response is not an ErrorResponse', async () => {
    server.use(
      http.get('/api/v1/pokemon', () =>
        HttpResponse.html('<html><body>502 Bad Gateway</body></html>', { status: 502 }),
      ),
    )

    const error = await request('/pokemon').catch((caught: unknown) => caught)

    expect(error).toBeInstanceOf(ApiError)
    expect(error).toMatchObject({
      status: 502,
      code: 'UNEXPECTED_RESPONSE',
      message: 'Something went wrong. Please try again.',
      fieldErrors: [],
    })
  })

  it('gives an ApiError when the server cannot be reached', async () => {
    server.use(http.get('/api/v1/pokemon', () => HttpResponse.error()))

    const error = await request('/pokemon').catch((caught: unknown) => caught)

    expect(error).toBeInstanceOf(ApiError)
    expect(error).toMatchObject({
      status: 0,
      code: 'NETWORK_ERROR',
      message: 'Could not reach the server. Check your connection and try again.',
    })
  })
})
