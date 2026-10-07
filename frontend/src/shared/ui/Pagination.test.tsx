import { render, screen } from '@testing-library/react'
import { createMemoryRouter } from 'react-router'
import { RouterProvider } from 'react-router/dom'
import { describe, expect, it } from 'vitest'
import { Pagination } from './Pagination'

function renderPagination(page: number, totalPages: number) {
  const router = createMemoryRouter([
    { path: '/', element: <Pagination page={page} totalPages={totalPages} hrefFor={(target) => `/?page=${target}`} /> },
  ])
  render(<RouterProvider router={router} />)
}

describe('Pagination', () => {
  it('has no previous link on the first page and no next link on the last', () => {
    renderPagination(1, 1)

    expect(screen.queryByRole('link', { name: 'Previous page' })).not.toBeInTheDocument()
    expect(screen.queryByRole('link', { name: 'Next page' })).not.toBeInTheDocument()
    expect(screen.getByText('Page 1 of 1')).toBeInTheDocument()
  })
})
