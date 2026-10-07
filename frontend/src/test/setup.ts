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
  // jsdom has no layout, so its scrollTo only logs "not implemented"; tests assert the calls instead.
  vi.spyOn(window, 'scrollTo').mockImplementation(() => {})
})

afterEach(() => {
  cleanup()
  sessionStorage.clear()
  server.resetHandlers()
  vi.restoreAllMocks()
  if (consoleOutput.length > 0) {
    throw new Error(`The test wrote to the console:\n${consoleOutput.join('\n')}`)
  }
})
