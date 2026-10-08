import { Link, Outlet, ScrollRestoration, useLocation } from 'react-router'
import { withReturnTo } from '../../features/auth/lib/authLink'
import { useAuth } from '../../features/auth/useAuth'
import { Button } from '../../shared/ui/Button'
import styles from './AppShell.module.css'

export function AppShell() {
  const { session, signOut } = useAuth()
  const { pathname, search } = useLocation()
  const here = pathname + search
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
                <Link to={withReturnTo('/login', here)}>Sign in</Link>
                <Link to={withReturnTo('/register', here)}>Create account</Link>
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
