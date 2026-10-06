# Frontend Standard — React + TypeScript + Vite

Binding rules for everything under `frontend/`. The brief sets three criteria for the frontend, and
every rule here serves one of them:

1. **Responsive, user-centred design.**
2. **CRUD matching the functional use cases.** Browse the list (US-01), open a detail (US-02), and
   on the detail page sync (US-03), edit the local fields (US-04) and remove. Plus
   register, login and logout.
3. **Architectural integrity:** clean component organization and efficient state management.

Plus the "optional but desired" criterion, which we treat as **mandatory**: **zero warnings or
errors in the browser console.**

Stack: React 19 · TypeScript (strict) · Vite · ESLint, plus the approved additions React Router
(D-020), TanStack Query (D-021), Vitest + React Testing Library + user-event + jsdom + MSW
(D-023) and jest-dom matchers (D-037). Form libraries were rejected (D-022). **Anything else needs a new decision first.**

---

## Structure: feature-first, layered inside

```
frontend/src
├── app/                    # composition: providers, router, layout shell, global styles
│   ├── App.tsx
│   ├── router.tsx
│   ├── providers.tsx       # QueryClientProvider, AuthProvider, Router
│   └── layout/             # AppShell, NavBar, Footer
├── features/
│   ├── pokemon/            # US-01…US-04 — one resource, two pages
│   │   ├── api/            # pokemonApi.ts: typed calls + DTO types (mirror backend responses)
│   │   ├── hooks/          # usePokemonPage, usePokemon, useSyncPokemon, useUpdateLocal, useRemoveLocal
│   │   ├── components/     # PokemonCard, StatBars, EvolutionTree, TypeBadge, LocalSection, LocalForm, TagInput
│   │   └── pages/          # PokemonListPage, PokemonDetailPage
│   └── auth/
│       ├── api/  lib/  pages/
│       ├── session.ts      # sessionStorage read/write, drops expired or malformed sessions
│       ├── authContext.ts  # the context and its type
│       ├── AuthProvider.tsx # session (token + user), signIn/signUp/signOut — the ONLY global client state
│       └── useAuth.ts      # split from the provider: fast refresh wants component files to export only components
├── shared/
│   ├── api/                # httpClient.ts (fetch wrapper), ApiError, PageResponse<T>, ErrorResponse
│   ├── ui/                 # global components, created when a slice first needs them (see "UX first, UI later")
│   ├── hooks/              # useDebouncedValue, useMediaQuery …
│   └── lib/                # pure helpers (formatters, kg/m display) — unit tested
└── test/                   # setup (MSW server, console guard), test utils (renderWithProviders)
```

Rules:
- **Dependency direction:** `app → features → shared`. A feature never imports from another
  feature's internals. If two features need the same thing, it moves to `shared/`. ESLint
  enforces this with `no-restricted-imports` patterns.
- **Pages** compose. **Components** render props and raise events, with no fetching inside them.
  **Hooks** own data access. **`api/`** owns HTTP. Each component does one of these, not two.
- One component per file, `PascalCase.tsx`. Hooks are `useCamelCase.ts`. Co-locate the test
  (`PokemonCard.test.tsx`) and the CSS module (`PokemonCard.module.css`).
- Named exports only. No default exports.

## State management

| Kind of state | Where it lives | Why |
|---|---|---|
| **Server state** (list pages, details with their local data) | **TanStack Query** cache | Caching, dedup, background refetch, loading/error states, and invalidation after mutations, without hand-written reducers |
| **Session** (access token, current user) | `AuthContext` (+ `sessionStorage` to survive a reload) | The only truly global client state |
| **URL state** (the list page number) | **Router search params** | Shareable, back-button friendly, survives reload |
| **Form state** | Local to the form: controlled inputs + a small pure `validate()` per form (D-022) | Never global |
| **UI state** (modal open, …) | `useState` in the owning component | Smallest possible scope |

No Redux/Zustand. Nothing needs it, and saying *why* is a good review answer. Never copy server
data into `useState`. Derive it instead.

Query keys are centralized per feature (`pokemonKeys.page(page, size)`,
`pokemonKeys.detail(identifier)`). Sync, update and remove invalidate that Pokémon's detail **and**
the list pages, because the list shows `displayName` and `synced`. The backend merges PokeAPI and
local data, so the frontend never stitches two sources together.

## HTTP layer (`shared/api/httpClient.ts`)

- One `fetch` wrapper. Base URL from `import.meta.env.VITE_API_BASE_URL` (default `/api/v1`), so
  the API prefix lives in exactly one place. The frontend only ever calls our backend, never
  PokeAPI.
- It attaches `Authorization: Bearer <token>` only on protected calls, and drops a token whose
  `expiresAt` has passed. Public routes ignore the header anyway (D-036), so sending it there would
  only leak the token into requests that don't need it.
- It parses every non-2xx response into a typed `ApiError { status, code, message, fieldErrors }`,
  using the backend's `ErrorResponse` shape. UI code branches on `code`/`status`, never on message
  text.
- A **401** on a protected call clears the session and redirects to login, keeping a `returnTo`.
  The login call's own 401 is a form error ("Invalid email or password"), never a redirect. The
  client only reports the 401 as an `ApiError`; the session layer (`AuthContext`) reacts to it, so
  `httpClient` knows nothing about sessions.
- A response that isn't an `ErrorResponse` (a proxy's HTML error page) becomes
  `ApiError(status, 'UNEXPECTED_RESPONSE')`, and no response at all becomes
  `ApiError(0, 'NETWORK_ERROR')`, so the UI always has one error type to handle.
- It never logs tokens or bodies.
- DTO types live in each feature's `api/` and mirror the backend responses exactly. When the API
  contract changes, these change in the same commit.

In dev, Vite proxies `/api` → `http://localhost:8080` (`vite.config.ts` `server.proxy`), so there's
no CORS in dev. In Docker, nginx proxies `/api` to the backend container.

## UX requirements (criterion #1)

- **Mobile-first and responsive.** CSS Grid/Flexbox, `rem` units, breakpoints at ~640/1024px, and
  no horizontal scroll at 360px width. The card grid goes 1 → 2 → 3–4 columns.
- **Every async view has all four states:** loading (skeletons, not spinners, for lists), error
  (an `ErrorState` with the backend message and a Retry button), empty (an `EmptyState` with a next
  action), and success.
- **Feedback for every mutation:** a disabled/pending button while submitting, a visible result
  on success (navigate to the updated item, or an inline success message with `role="status"`), and
  inline field errors from `fieldErrors` on a 400. No toast system, since that's out of scope.
- **Destructive actions confirm** (delete), with an accessible dialog.
- **Accessibility:** semantic HTML (`<main>`, `<nav>`, `<button>` for actions, `<a>` for
  navigation), labelled inputs, `alt` text on sprites (`"Pikachu sprite"`), visible focus, AA
  colour contrast, and keyboard-only flows that work.
- **Images:** explicit `width`/`height` (no layout shift), `loading="lazy"` in grids, and a
  fallback when a sprite URL is `null`.
- Pagination is reflected in the URL (`?page=3`).
- Styling uses CSS Modules + CSS custom properties (design tokens in `app/styles/tokens.css`), with
  type colours as tokens. No UI library. Dark mode is out of scope.

## UX first, UI later (D-034)

The slices build a **functional, usable MVP** with minimal styling. A dedicated **UI pass** at the
end (plan → UI pass) makes it look good. The two are separate on purpose:

- **UX is never deferred.** Everything in "UX requirements" above ships with the slice that needs
  it: responsive layout, the four async states, field errors, confirmations, keyboard use, visible
  focus, readable contrast.
- **UI is deferred.** That covers the colour palette, the typeface, refined spacing, type colours,
  imagery and visual detail. The MVP is plain and tidy, not ugly: system font, a neutral grey
  palette, a consistent spacing scale.
- **Design tokens from day one.** Every colour, font, size, spacing, radius and shadow is a CSS
  custom property in `app/styles/tokens.css`. Components only ever read tokens, never literal
  values. The UI pass then changes token values first and component styles second, without hunting
  colours through pages.
- **Global components from day one, created on demand.** Anything used twice lives in
  `shared/ui`, created by the first slice that needs it, never ahead of time: `Button`,
  `TextField` (label + input + error), `Heading`, `Stack` (spacing between elements),
  `ErrorState`, then `Card`, `Skeleton`, `Pagination`, `EmptyState`, `Badge`, `ConfirmDialog`.
- **Pages compose, components style.** No inline styles and no literal colours in pages or feature
  components. If a page needs a look that doesn't exist, it becomes a variant of a global component
  (`<Button variant="danger">`), not a one-off CSS rule.

## Zero console warnings (treated as mandatory)

Common sources, all forbidden:
- Missing/unstable `key`s in lists (use `pokedexNumber`/`id`, never the index).
- Controlled ↔ uncontrolled input switches (initialize every field, `''` not `undefined`).
- State updates on unmounted components, and effects without cleanup (`AbortController` — TanStack
  Query handles it for queries).
- Effects that misbehave under `StrictMode`'s double invocation. Keep `StrictMode` **on**.
- `act(...)` warnings in tests. Use Testing Library's async `findBy*`/`waitFor`.
- Router future-flag warnings, React DOM nesting warnings (`<div>` in `<p>`), missing `alt`, and
  404s on images or favicons.
- **Guard in tests:** `src/test/setup.ts` makes any `console.error`/`console.warn` during a test
  **fail that test**.
- **Guard before demo:** a manual pass through every page with DevTools open, recorded in
  `plan.md`.

## Testing (TDD applies here too)

- **Vitest + React Testing Library + MSW** (D-023), with jest-dom matchers (D-037). The MSW server
  (`src/test/msw/server.ts`) fails any request without a handler. Handlers mirror the backend
  contract (`ErrorResponse`, `PageResponse`) and live in `src/test/msw/handlers.ts`, created with the
  first one. No test globals: `describe`/`it`/`expect` are imported from `vitest`.
- Test **behaviour through the UI** the way a user does: `getByRole`, `getByLabelText`, then
  `userEvent`. Don't test implementation details (state, hooks internals, CSS classes).
- Minimum per feature: the page renders its data, the loading → success path, an error path (500
  → ErrorState, 400 → field errors), the empty state, and the main mutation (edit saves, delete
  confirms and removes, sync shows a 409 message).
- Pure helpers in `shared/lib` get plain unit tests.
- `npm run lint`, `npm run typecheck` (`tsc -b`), `npm test -- --run` and `npm run build` must all
  pass with zero warnings before a task counts as done.

## TypeScript / code rules

- `strict: true`, no `any` (use `unknown` + narrowing), no non-null `!` assertions on data that
  could genuinely be absent. That's the frontend form of "never silently unwrap".
- Model API absence explicitly: `spriteUrl: string | null`, and the UI handles `null`.
- Code, identifiers, comments and UI copy are in English (the brief is in English). Prefer
  descriptive names over comments.
- No `console.log` left in committed code.
- Environment config only via `import.meta.env.VITE_*`, with `.env.example` committed.

## Pages and routing

Two pages for the product, plus auth (D-030).

| Path | Page | Access |
|---|---|---|
| `/?page=` | **PokemonListPage**: cards with sprite, display name (original name small underneath when different), number, category, weight in kg, abilities, a "synced" badge (US-01) | public |
| `/pokemon/:identifier` | **PokemonDetailPage**: artwork, stats, description, evolution tree (US-02), plus the **local section** | public view |
| `/login`, `/register` | auth | public |
| `*` | NotFoundPage | public |

The **local section** of the detail page decides everything from `local` in the response:
- `local === null`: shows **"Sync to local database"** (US-03). Logged out, the button reads "Log in
  to sync" and goes to `/login` with a `returnTo`.
- `local !== null`: shows the localized name, region and tags, with **Edit** (an inline form, US-04)
  and **Remove** (confirm dialog). Logged out, it shows the data without the buttons.

Sync → 409 ("already synced", e.g. someone else just did it) refetches the detail and shows the
existing local data, with an inline note. It's not treated as a generic error.

A successful sign up signs the user in straight away (register, then login) and returns to
`returnTo`, or to the list. There's no separate "now sign in" step.

No `RequireAuth` route wrapper is needed: no page is private, only actions are. Pages are imported
eagerly. The bundle is small, and code splitting is out of scope.
