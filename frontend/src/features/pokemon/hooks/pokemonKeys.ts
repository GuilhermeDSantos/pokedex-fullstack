export const pokemonKeys = {
  all: ['pokemon'] as const,
  pages: () => [...pokemonKeys.all, 'page'] as const,
  page: (page: number, size: number) => [...pokemonKeys.pages(), page, size] as const,
  details: () => [...pokemonKeys.all, 'detail'] as const,
  detail: (identifier: string) => [...pokemonKeys.details(), identifier] as const,
}
