import { useState, type ReactNode } from 'react'
import { fetchCurrentUser, login, register, type Credentials, type Registration } from './api/authApi'
import { AuthContext } from './authContext'
import { clearStoredSession, readStoredSession, storeSession } from './session'

export function AuthProvider({ children }: { children: ReactNode }) {
  const [session, setSession] = useState(readStoredSession)

  const signIn = async (credentials: Credentials) => {
    const token = await login(credentials)
    const user = await fetchCurrentUser(token.accessToken)
    const signedIn = { accessToken: token.accessToken, expiresAt: token.expiresAt, user }
    storeSession(signedIn)
    setSession(signedIn)
  }

  const signUp = async (registration: Registration) => {
    await register(registration)
    await signIn({ email: registration.email, password: registration.password })
  }

  const signOut = () => {
    clearStoredSession()
    setSession(null)
  }

  return <AuthContext value={{ session, signIn, signUp, signOut }}>{children}</AuthContext>
}
