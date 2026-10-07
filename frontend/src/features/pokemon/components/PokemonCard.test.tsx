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
})
