// PokeAPI names are lower-case slugs ("lightning-rod"); people read "Lightning Rod".
export function formatName(slug: string): string {
  return slug
    .split('-')
    .map((word) => word.charAt(0).toUpperCase() + word.slice(1))
    .join(' ')
}

export function formatPokedexNumber(pokedexNumber: number): string {
  return `#${String(pokedexNumber).padStart(3, '0')}`
}

// JSON drops the trailing zero (6.0 arrives as 6), so the one decimal is put back for display.
export function formatKilograms(kilograms: number): string {
  return `${kilograms.toFixed(1)} kg`
}
