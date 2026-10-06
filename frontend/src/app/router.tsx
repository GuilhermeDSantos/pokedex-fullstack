import type { RouteObject } from 'react-router'
import { AppShell } from './layout/AppShell'
import { NotFoundPage } from './pages/NotFoundPage'

export const routes: RouteObject[] = [
  {
    path: '/',
    element: <AppShell />,
    children: [{ path: '*', element: <NotFoundPage /> }],
  },
]
