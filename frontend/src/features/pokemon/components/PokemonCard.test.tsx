import { render, screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import { PIKACHU } from '../../../test/fixtures/pokemon'
import { PokemonCard } from './PokemonCard'

describe('PokemonCard', () => {
  it('shows a placeholder instead of a broken image when PokeAPI has no sprite', () => {
    render(<PokemonCard pokemon={{ ...PIKACHU, spriteUrl: null }} />)

    expect(screen.queryByRole('img')).not.toBeInTheDocument()
    expect(screen.getByText('No image')).toBeInTheDocument()
  })

  it('says the category is unknown when PokeAPI has no English one', () => {
    render(<PokemonCard pokemon={{ ...PIKACHU, category: null }} />)

    expect(screen.getByText('Category unknown')).toBeInTheDocument()
  })
})
