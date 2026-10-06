import { Link, Outlet } from 'react-router'

export function AppShell() {
  return (
    <>
      <header>
        <Link to="/">Pokémon Catalog</Link>
      </header>
      <main>
        <Outlet />
      </main>
    </>
  )
}
