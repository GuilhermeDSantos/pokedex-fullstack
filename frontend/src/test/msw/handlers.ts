import { http, HttpResponse } from 'msw'
import { BULBASAUR, PIKACHU, PIKACHU_DETAIL, pageOf } from '../fixtures/pokemon'

// Default answers, shaped like the real API, for pages a test renders without caring about them.
export const handlers = [
  http.get('/api/v1/pokemon', () => HttpResponse.json(pageOf([BULBASAUR, PIKACHU]))),
  http.get('/api/v1/pokemon/:identifier', () => HttpResponse.json(PIKACHU_DETAIL)),
]
