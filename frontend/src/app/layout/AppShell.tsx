import { Link, Outlet } from 'react-router'
import styles from './AppShell.module.css'

export function AppShell() {
  return (
    <>
      <header className={styles.header}>
        <div className={styles.headerContent}>
          <Link to="/" className={styles.brand}>
            Pokémon Catalog
          </Link>
          <nav aria-label="Account" className={styles.account}>
            <Link to="/login">Sign in</Link>
            <Link to="/register">Create account</Link>
          </nav>
        </div>
      </header>
      <main className={styles.main}>
        <Outlet />
      </main>
    </>
  )
}
