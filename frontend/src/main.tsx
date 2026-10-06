import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'

const container = document.getElementById('root')
if (!container) {
  throw new Error('index.html must have a #root element')
}

createRoot(container).render(<StrictMode />)
