export const pokemonKeys = {
  all: ['pokemon'] as const,
  page: (page: number, size: number) => [...pokemonKeys.all, 'page', page, size] as const,
}
