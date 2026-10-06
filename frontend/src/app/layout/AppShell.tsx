import { Link, Outlet } from 'react-router'
import { useAuth } from '../../features/auth/useAuth'
import { Button } from '../../shared/ui/Button'
import styles from './AppShell.module.css'

export function AppShell() {
  const { session, signOut } = useAuth()
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
                <Link to="/login">Sign in</Link>
                <Link to="/register">Create account</Link>
              </>
            )}
          </nav>
        </div>
      </header>
      <main className={styles.main}>
        <Outlet />
      </main>
    </>
  )
}
