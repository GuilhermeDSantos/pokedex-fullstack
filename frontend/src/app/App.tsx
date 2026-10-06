import { useState } from 'react'
import { createBrowserRouter } from 'react-router'
import { RouterProvider } from 'react-router/dom'
import { Providers } from './providers'
import { routes } from './router'

export function App() {
  const [router] = useState(() => createBrowserRouter(routes))
  return (
    <Providers>
      <RouterProvider router={router} />
    </Providers>
  )
}
