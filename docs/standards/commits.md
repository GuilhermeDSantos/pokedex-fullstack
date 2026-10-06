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

## TDD rhythm in history

Either commit each step:

```
test(domain): specify that updating custom attributes keeps the profile
feat(domain): implement Pokemon.updateCustomAttributes
refactor(domain): move blank-to-null normalization into CustomAttributes
```

…or one `feat` per green cycle that includes its tests. Never a `feat` whose behaviour has no test
in the same commit or the one before it.

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
