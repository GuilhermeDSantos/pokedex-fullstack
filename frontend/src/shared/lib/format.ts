// PokeAPI names are lower-case slugs ("lightning-rod"); people read "Lightning Rod".
export function formatName(slug: string): string {
  return slug
    .split('-')
    .map((word) => word.charAt(0).toUpperCase() + word.slice(1))
    .join(' ')
}
