import { createContext } from 'react'
import type { Session } from './session'

export type AuthContextValue = {
  session: Session | null
  signOut: () => void
}

export const AuthContext = createContext<AuthContextValue | null>(null)
