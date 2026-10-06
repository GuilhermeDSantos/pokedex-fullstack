import { render, screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import { Heading } from './Heading'

describe('Heading', () => {
  it('renders the requested heading level', () => {
    render(<Heading level={2}>Local data</Heading>)

    expect(screen.getByRole('heading', { level: 2, name: 'Local data' })).toBeInTheDocument()
  })
})
