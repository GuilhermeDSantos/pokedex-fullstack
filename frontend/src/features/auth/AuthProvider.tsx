import { useState, type ReactNode } from 'react'
import { AuthContext } from './authContext'
import { clearStoredSession, readStoredSession } from './session'

export function AuthProvider({ children }: { children: ReactNode }) {
  const [session, setSession] = useState(readStoredSession)

  const signOut = () => {
    clearStoredSession()
    setSession(null)
  }

  return <AuthContext value={{ session, signOut }}>{children}</AuthContext>
}
