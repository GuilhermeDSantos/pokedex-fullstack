import { Link } from 'react-router'
import styles from './Pagination.module.css'

type PaginationProps = {
  page: number
  totalPages: number
  hrefFor: (page: number) => string
}

// Links, not buttons: each page has its own URL, so it can be shared and the back button works.
export function Pagination({ page, totalPages, hrefFor }: PaginationProps) {
  return (
    <nav aria-label="Pagination" className={styles.pagination}>
      <Link to={hrefFor(page - 1)}>Previous page</Link>
      <span>
        Page {page} of {totalPages}
      </span>
      <Link to={hrefFor(page + 1)}>Next page</Link>
    </nav>
  )
}
