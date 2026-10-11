import { useEffect, useState } from 'react'
import { clearToken, getToken, onUnauthorized, saveToken } from './api.ts'
import LoginPage from './pages/LoginPage.tsx'
import CatalogPage from './pages/CatalogPage.tsx'
import CollectionPage from './pages/CollectionPage.tsx'

type Tab = 'catalog' | 'collection'

export default function App() {
  // The token is the "am I logged in?" state. It starts with what was saved in the browser.
  const [token, setToken] = useState<string | null>(getToken())
  const [tab, setTab] = useState<Tab>('catalog')

  // If the backend answers 401 (token expired), go back to the login screen.
  useEffect(() => {
    onUnauthorized(() => setToken(null))
  }, [])

  function handleLogin(newToken: string) {
    saveToken(newToken)
    setToken(newToken)
  }

  function handleLogout() {
    clearToken()
    setToken(null)
  }

  if (!token) {
    return <LoginPage onLogin={handleLogin} />
  }

  return (
    <>
      <header className="app-header">
        <h1 className="brand">Pokédex</h1>
        <nav className="tabs">
          <button
            className={tab === 'catalog' ? 'tab active' : 'tab'}
            onClick={() => setTab('catalog')}
          >
            All Pokémon
          </button>
          <button
            className={tab === 'collection' ? 'tab active' : 'tab'}
            onClick={() => setTab('collection')}
          >
            My collection
          </button>
        </nav>
        <button className="link-button" onClick={handleLogout}>
          Sign out
        </button>
      </header>

      <main className="content">
        {tab === 'catalog' ? <CatalogPage /> : <CollectionPage />}
      </main>
    </>
  )
}
