import type { RouteObject } from 'react-router'
import { AppShell } from './layout/AppShell'

export const routes: RouteObject[] = [
  {
    path: '/',
    element: <AppShell />,
  },
]
