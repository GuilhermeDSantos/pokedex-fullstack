import { Link, Outlet, ScrollRestoration, useLocation, useSearchParams } from 'react-router'
import { withReturnTo } from '../../features/auth/lib/authLink'
import { useAuth } from '../../features/auth/useAuth'
import { Button } from '../../shared/ui/Button'
import styles from './AppShell.module.css'

export function AppShell() {
  const { session, signOut } = useAuth()
  const { pathname, search } = useLocation()
  const [searchParams] = useSearchParams()
  // On sign in or sign up, keep where the user was going instead of returning to the form itself.
  const onAuthPage = pathname === '/login' || pathname === '/register'
  const returnTo = onAuthPage ? searchParams.get('returnTo') : pathname + search
  return (
    <>
      <header className={styles.header}>
        <div className={styles.headerContent}>
          <Link to="/" className={styles.brand}>
            Pokémon Catalog
          </Link>
          <nav aria-label="Account" className={styles.account}>
            {session ? (
              <>
                <span>{session.user.name}</span>
                <Button onClick={signOut}>Sign out</Button>
              </>
            ) : (
              <>
                <Link to={withReturnTo('/login', returnTo)}>Sign in</Link>
                <Link to={withReturnTo('/register', returnTo)}>Create account</Link>
              </>
            )}
          </nav>
        </div>
      </header>
      <main className={styles.main}>
        <Outlet />
      </main>
      <ScrollRestoration />
    </>
  )
}
