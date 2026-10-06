import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { App } from './app/App'
import './app/styles/tokens.css'
import './app/styles/base.css'

const container = document.getElementById('root')
if (!container) {
  throw new Error('index.html must have a #root element')
}

createRoot(container).render(
  <StrictMode>
    <App />
  </StrictMode>,
)
