import { createContext } from 'react'
import type { Credentials, Registration } from './api/authApi'
import type { Session } from './session'

export type AuthContextValue = {
  session: Session | null
  signIn: (credentials: Credentials) => Promise<void>
  signUp: (registration: Registration) => Promise<void>
  signOut: () => void
}

export const AuthContext = createContext<AuthContextValue | null>(null)
