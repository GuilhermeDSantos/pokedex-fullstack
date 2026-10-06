import '@testing-library/jest-dom/vitest'
import { cleanup } from '@testing-library/react'
import { afterAll, afterEach, beforeAll, beforeEach, vi } from 'vitest'
import { server } from './msw/server'

beforeAll(() => server.listen({ onUnhandledRequest: 'error' }))
afterAll(() => server.close())

// Zero console warnings is a requirement, so any console.error or console.warn fails the test.
let consoleOutput: string[] = []

beforeEach(() => {
  consoleOutput = []
  const record = (...args: unknown[]) => consoleOutput.push(args.map(String).join(' '))
  vi.spyOn(console, 'error').mockImplementation(record)
  vi.spyOn(console, 'warn').mockImplementation(record)
})

afterEach(() => {
  cleanup()
  server.resetHandlers()
  vi.restoreAllMocks()
  if (consoleOutput.length > 0) {
    throw new Error(`The test wrote to the console:\n${consoleOutput.join('\n')}`)
  }
})
