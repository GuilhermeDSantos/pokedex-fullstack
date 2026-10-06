import type { User } from './api/authApi'

export type Session = {
  accessToken: string
  expiresAt: string
  user: User
}

const STORAGE_KEY = 'pokemon-catalog.session'

export function storeSession(session: Session) {
  sessionStorage.setItem(STORAGE_KEY, JSON.stringify(session))
}

export function readStoredSession(): Session | null {
  const stored = sessionStorage.getItem(STORAGE_KEY)
  if (stored === null) {
    return null
  }
  const parsed = parseJson(stored)
  if (!isSession(parsed) || Date.parse(parsed.expiresAt) <= Date.now()) {
    clearStoredSession()
    return null
  }
  return parsed
}

export function clearStoredSession() {
  sessionStorage.removeItem(STORAGE_KEY)
}

function parseJson(text: string): unknown {
  try {
    return JSON.parse(text)
  } catch {
    return null
  }
}

function isSession(value: unknown): value is Session {
  return (
    typeof value === 'object' &&
    value !== null &&
    'accessToken' in value &&
    typeof value.accessToken === 'string' &&
    'expiresAt' in value &&
    typeof value.expiresAt === 'string' &&
    'user' in value &&
    typeof value.user === 'object' &&
    value.user !== null &&
    'name' in value.user &&
    typeof value.user.name === 'string'
  )
}
