import { useState } from 'react'
import type { SubmitEvent } from 'react'
import { api, toApiError } from '../api.ts'

interface LoginPageProps {
  onLogin: (token: string) => void
}

export default function LoginPage({ onLogin }: LoginPageProps) {
  const [mode, setMode] = useState<'signin' | 'register'>('signin')
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({})
  const [busy, setBusy] = useState(false)

  const isRegister = mode === 'register'

  async function handleSubmit(event: SubmitEvent<HTMLFormElement>) {
    event.preventDefault()
    setBusy(true)
    setError(null)
    setFieldErrors({})
    try {
      if (isRegister) {
        await api.register(username, password)
      }
      const { accessToken } = await api.login(username, password)
      onLogin(accessToken)
    } catch (err) {
      const apiError = toApiError(err)
      setError(apiError.message)
      setFieldErrors(apiError.fieldErrors)
    } finally {
      setBusy(false)
    }
  }

  function switchMode() {
    setMode(isRegister ? 'signin' : 'register')
    setError(null)
    setFieldErrors({})
  }

  return (
    <main className="login">
      <form className="login-card" onSubmit={handleSubmit}>
        <h1 className="brand">Pokédex</h1>
        <p className="muted">
          {isRegister ? 'Create an account to start your collection.' : 'Sign in to see your collection.'}
        </p>

        <label>
          Username
          <input
            value={username}
            onChange={(e) => setUsername(e.target.value)}
            autoComplete="username"
            required
          />
          {fieldErrors.username && <span className="field-error">{fieldErrors.username}</span>}
        </label>

        <label>
          Password
          <input
            type="password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            autoComplete={isRegister ? 'new-password' : 'current-password'}
            required
          />
          {fieldErrors.password && <span className="field-error">{fieldErrors.password}</span>}
        </label>

        {/* when the backend says which fields are wrong, those messages are enough */}
        {error && Object.keys(fieldErrors).length === 0 && <p className="error">{error}</p>}

        <button className="primary" type="submit" disabled={busy}>
          {isRegister ? 'Create account' : 'Sign in'}
        </button>
        <button className="link-button" type="button" onClick={switchMode}>
          {isRegister ? 'I already have an account' : 'Create an account'}
        </button>
      </form>
    </main>
  )
}
