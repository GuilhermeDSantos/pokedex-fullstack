const START_PAGE = '/'

// Only same-app paths: "//host" is protocol-relative and would leave the site (open redirect).
export function safeReturnTo(value: string | null): string {
  if (value === null || !value.startsWith('/') || value.startsWith('//')) {
    return START_PAGE
  }
  return value
}
