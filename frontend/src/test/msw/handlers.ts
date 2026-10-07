import { http, HttpResponse } from 'msw'
import { BULBASAUR, PIKACHU, pageOf } from '../fixtures/pokemon'

// Default answers, shaped like the real API, for pages a test renders without caring about them.
export const handlers = [http.get('/api/v1/pokemon', () => HttpResponse.json(pageOf([BULBASAUR, PIKACHU])))]
