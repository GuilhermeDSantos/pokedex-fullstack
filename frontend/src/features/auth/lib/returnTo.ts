const START_PAGE = '/'

export function safeReturnTo(value: string | null): string {
  return value ?? START_PAGE
}
