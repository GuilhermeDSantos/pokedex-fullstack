import { useState, type ReactNode } from 'react'
import { AuthContext } from './authContext'
import { readStoredSession } from './session'

export function AuthProvider({ children }: { children: ReactNode }) {
  const [session] = useState(readStoredSession)
  return <AuthContext value={{ session }}>{children}</AuthContext>
}
