const START_PAGE = '/'

// Only same-app paths. "//host" and "/\host" both leave the site: browsers read "\" as "/".
export function safeReturnTo(value: string | null): string {
  if (value === null || !value.startsWith('/') || value.startsWith('//') || value.startsWith('/\\')) {
    return START_PAGE
  }
  return value
}
