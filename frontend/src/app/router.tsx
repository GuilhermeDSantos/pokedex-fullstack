import type { RouteObject } from 'react-router'
import { SignInPage } from '../features/auth/pages/SignInPage'
import { SignUpPage } from '../features/auth/pages/SignUpPage'
import { PokemonListPage } from '../features/pokemon/pages/PokemonListPage'
import { AppShell } from './layout/AppShell'
import { NotFoundPage } from './pages/NotFoundPage'

export const routes: RouteObject[] = [
  {
    path: '/',
    element: <AppShell />,
    children: [
      { index: true, element: <PokemonListPage /> },
      { path: 'login', element: <SignInPage /> },
      { path: 'register', element: <SignUpPage /> },
      { path: '*', element: <NotFoundPage /> },
    ],
  },
]
