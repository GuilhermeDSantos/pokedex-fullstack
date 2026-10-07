export const pokemonKeys = {
  all: ['pokemon'] as const,
  page: (page: number, size: number) => [...pokemonKeys.all, 'page', page, size] as const,
  details: () => [...pokemonKeys.all, 'detail'] as const,
  detail: (identifier: string) => [...pokemonKeys.details(), identifier] as const,
}
