# Commit Standard — Conventional Commits

Spec: https://www.conventionalcommits.org/. The commit history is part of the deliverable, because it
is the visible proof of TDD (OV-3, EV-2). Keep commits small, one concern each, and in an order that
tells the story.

## Format

```
<type>(<scope>): <description>

[body — why, not what]

[footer(s)]
```

## Types (lowercase, nothing else)

| Type | Use for |
|---|---|
| `feat` | New behaviour visible to an API consumer or UI user |
| `fix` | Bug fix |
| `test` | Adding or correcting tests only — including the **red** step of a TDD cycle when committed separately |
| `refactor` | Restructure without behaviour change (the TDD refactor step) |
| `docs` | Documentation only (`docs/`, README, Javadoc) |
| `build` | Gradle, npm, Docker, dependencies |
| `ci` | CI configuration |
| `perf` | Performance, no behaviour change |
| `style` | Formatting only |
| `chore` | Anything else that touches neither source nor tests |
| `revert` | Reverts a previous commit |

## Scopes used in this repo

`domain`, `app` (application layer), `persistence`, `pokeapi`, `security`, `api` (interfaces/rest),
`catalog`, `pokedex`, `auth` (feature-level, frontend or backend), `fe` (frontend-wide), `docker`,
`docs`, `arch` (ArchUnit). Leave the scope out when a change honestly spans several areas.

## Description

Imperative mood ("add", not "added"), lowercase first letter, no trailing period, ≤ 72 chars.

## Body

Explain **why**, especially for decisions ("see D-012"). Reference requirement IDs:
`Refs: US-04, US-04.b`.

## TDD rhythm in history: one commit per step

Every TDD cycle shows up in the history as separate commits, one per step, so the order
(test first) is visible:

```
test(domain): specify that a synced pokemon starts without custom attributes    ← red
feat(domain): create LocalPokemon with empty custom attributes                  ← green
refactor(domain): extract the empty-attributes factory                          ← only if there is one
```

- **Red** (`test:`): only the new failing test, no production code. The build fails, for the
  reason the test describes.
- **Green** (`feat:`/`fix:`): the minimum production code that makes it pass. The test isn't touched.
- **Refactor** (`refactor:`): only when there is something to improve. Behaviour doesn't change, and
  neither do the tests.
- **One cycle per behaviour**, not per method or per line.
- **Never push while red.** Red commits exist in the history, but `git push` only happens on a
  green build, so the remote branch is never broken.
- A test changes only in its own commit, for a stated reason: the expected behaviour changed, the
  test was wrong, or it's a readability refactor of the test code (`test:` or `refactor(test):`).
  Never change a test to make production code pass.

## Footer

- `Refs: US-03, TR-API-1`: requirement IDs from `docs/requirements.md`.
- `BREAKING CHANGE: …` if an API contract changes (it shouldn't. Add a `/api/v2` endpoint instead, D-029).
- The AI co-author trailer, as configured for the tool in use.

## Examples

```
feat(pokeapi): translate species genus into catalog category

PokeAPI has no "category" field; the Pokédex category is the English genus
of the species resource (D-010).

Refs: US-01
```

```
fix(api): return 400 instead of 500 for malformed JSON bodies

HttpMessageNotReadableException fell through to the catch-all handler.

Refs: US-04.b
```
