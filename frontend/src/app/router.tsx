import type { RouteObject } from 'react-router'
import { SignInPage } from '../features/auth/pages/SignInPage'
import { AppShell } from './layout/AppShell'
import { NotFoundPage } from './pages/NotFoundPage'

export const routes: RouteObject[] = [
  {
    path: '/',
    element: <AppShell />,
    children: [
      { path: 'login', element: <SignInPage /> },
      { path: '*', element: <NotFoundPage /> },
    ],
  },
]
