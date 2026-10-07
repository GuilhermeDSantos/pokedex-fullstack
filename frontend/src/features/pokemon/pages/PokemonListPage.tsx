import { Link, useSearchParams } from 'react-router'
import { ErrorState } from '../../../shared/ui/ErrorState'
import { EmptyState } from '../../../shared/ui/EmptyState'
import { Heading } from '../../../shared/ui/Heading'
import { Pagination } from '../../../shared/ui/Pagination'
import { Skeleton } from '../../../shared/ui/Skeleton'
import { Stack } from '../../../shared/ui/Stack'
import { PokemonCard } from '../components/PokemonCard'
import { usePokemonPage } from '../hooks/usePokemonPage'
import styles from './PokemonListPage.module.css'

const PAGE_SIZE = 20

// The URL is typed by people: anything but a whole number from 1 up means the first page.
function pageFromUrl(value: string | null): number {
  const page = Number(value)
  return Number.isInteger(page) && page >= 1 ? page : 1
}

export function PokemonListPage() {
  const [searchParams] = useSearchParams()
  const page = pageFromUrl(searchParams.get('page'))
  const { data, isPending, error, refetch } = usePokemonPage(page - 1, PAGE_SIZE)
  return (
    <Stack gap={6}>
      <Heading level={1}>Pokémon</Heading>
      {isPending && (
        <div role="status" aria-label="Loading Pokémon">
          <ul className={styles.grid}>
            {Array.from({ length: PAGE_SIZE }, (_, slot) => (
              <li key={slot}>
                <Skeleton variant="card" />
              </li>
            ))}
          </ul>
        </div>
      )}
      {error && <ErrorState message={error.message} onRetry={() => void refetch()} />}
      {data?.content.length === 0 && (
        <EmptyState message="No Pokémon on this page." action={<Link to="/">Go to the first page</Link>} />
      )}
      {data && data.content.length > 0 && (
        <ul className={styles.grid}>
          {data.content.map((pokemon) => (
            <li key={pokemon.pokedexNumber}>
              <PokemonCard pokemon={pokemon} />
            </li>
          ))}
        </ul>
      )}
      {data && data.content.length > 0 && (
        <Pagination page={page} totalPages={data.totalPages} hrefFor={(target) => `/?page=${target}`} />
      )}
    </Stack>
  )
}
