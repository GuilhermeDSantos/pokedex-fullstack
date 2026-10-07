import styles from './Pagination.module.css'

type PaginationProps = {
  page: number
  totalPages: number
}

export function Pagination({ page, totalPages }: PaginationProps) {
  return (
    <nav aria-label="Pagination" className={styles.pagination}>
      <span>
        Page {page} of {totalPages}
      </span>
    </nav>
  )
}
