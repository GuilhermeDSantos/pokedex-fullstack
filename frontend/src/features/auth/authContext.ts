import { createContext } from 'react'
import type { Credentials } from './api/authApi'
import type { Session } from './session'

export type AuthContextValue = {
  session: Session | null
  signIn: (credentials: Credentials) => Promise<void>
  signOut: () => void
}

export const AuthContext = createContext<AuthContextValue | null>(null)
