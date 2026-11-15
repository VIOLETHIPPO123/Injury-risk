import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import './index.css'
import App from './App.jsx'
import { consumeOAuthRedirect } from './services/oauthRedirect'

// Once, before the first render: if this page load is the return from Google sign-in, store the
// token and clean it out of the address bar. Kept out of React, which may render App twice.
const oauthResult = consumeOAuthRedirect()

createRoot(document.getElementById('root')).render(
  <StrictMode>
    <App oauthResult={oauthResult} />
  </StrictMode>,
)
