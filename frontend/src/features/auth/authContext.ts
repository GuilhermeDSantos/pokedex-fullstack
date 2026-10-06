import { createContext } from 'react'
import type { Session } from './session'

export type AuthContextValue = {
  session: Session | null
}

export const AuthContext = createContext<AuthContextValue | null>(null)
