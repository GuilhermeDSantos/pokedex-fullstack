export function withReturnTo(path: '/login' | '/register', returnTo: string | null): string {
  return returnTo === null ? path : `${path}?${new URLSearchParams({ returnTo })}`
}
