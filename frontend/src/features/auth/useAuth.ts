import { useContext } from 'react'
import { AuthContext, type AuthContextValue } from './authContext'

export function useAuth(): AuthContextValue {
  const auth = useContext(AuthContext)
  if (auth === null) {
    throw new Error('useAuth must be used inside an AuthProvider')
  }
  return auth
}
