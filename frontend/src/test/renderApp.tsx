import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render } from '@testing-library/react'
import { createMemoryRouter } from 'react-router'
import { RouterProvider } from 'react-router/dom'
import { routes } from '../app/router'
import { AuthProvider } from '../features/auth/AuthProvider'
import { storeSession, type Session } from '../features/auth/session'

type RenderAppOptions = {
  session?: Session
}

// The real route table, so page tests also cover routing and the shell.
export function renderApp(path = '/', { session }: RenderAppOptions = {}) {
  if (session) {
    storeSession(session)
  }
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
  })
  const router = createMemoryRouter(routes, { initialEntries: [path] })
  render(
    <QueryClientProvider client={queryClient}>
      <AuthProvider>
        <RouterProvider router={router} />
      </AuthProvider>
    </QueryClientProvider>,
  )
  return { router }
}
