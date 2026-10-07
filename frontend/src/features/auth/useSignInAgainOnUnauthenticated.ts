import { useLocation, useNavigate } from 'react-router'
import { ApiError } from '../../shared/api/ApiError'
import { withReturnTo } from './lib/authLink'
import { useAuth } from './useAuth'

// A 401 on a write means the token expired or the account is gone (D-033): end the session and
// come back to this page after signing in.
export function useSignInAgainOnUnauthenticated() {
  const { signOut } = useAuth()
  const navigate = useNavigate()
  const { pathname } = useLocation()
  return (error: Error) => {
    if (error instanceof ApiError && error.status === 401) {
      signOut()
      void navigate(withReturnTo('/login', pathname))
    }
  }
}
