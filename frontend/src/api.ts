// All calls to our Spring Boot API live here, so the components never deal with fetch, headers or tokens.
import type {
  CustomFields,
  LocalPokemon,
  Page,
  PokemonDetail,
  PokemonSummary,
  TokenResponse,
  UserResponse,
} from './types.ts'

const TOKEN_KEY = 'pokedex.token'

export function getToken(): string | null {
  return localStorage.getItem(TOKEN_KEY)
}

export function saveToken(token: string): void {
  localStorage.setItem(TOKEN_KEY, token)
}

export function clearToken(): void {
  localStorage.removeItem(TOKEN_KEY)
}

// App.tsx registers what to do when the token expires (go back to the login screen).
let unauthorizedHandler: () => void = () => {}

export function onUnauthorized(handler: () => void): void {
  unauthorizedHandler = handler
}

/**
 * Error with the HTTP status and, for 400, the invalid fields: { username: "size must be..." }.
 * The backend sends errors as ProblemDetail: { status, title, detail, errors }.
 */
export class ApiError extends Error {
  status: number
  fieldErrors: Record<string, string>

  constructor(status: number, message: string, fieldErrors: Record<string, string> = {}) {
    super(message)
    this.status = status
    this.fieldErrors = fieldErrors
  }
}

/**
 * In a "catch", TypeScript does not know what was thrown (its type is "unknown").
 * This turns anything into an ApiError so the components can read .status, .message and .fieldErrors.
 */
export function toApiError(error: unknown): ApiError {
  return error instanceof ApiError ? error : new ApiError(0, String(error))
}

interface RequestOptions {
  method?: 'GET' | 'POST' | 'PUT' | 'DELETE'
  body?: unknown
}

// <T> is the type of the answer: request<Page<PokemonSummary>>(...) returns a Page<PokemonSummary>.
async function request<T>(path: string, { method = 'GET', body }: RequestOptions = {}): Promise<T> {
  const headers: Record<string, string> = {}
  if (body !== undefined) {
    headers['Content-Type'] = 'application/json'
  }
  const token = getToken()
  if (token) {
    headers.Authorization = `Bearer ${token}`
  }

  let response: Response
  try {
    response = await fetch(`/api${path}`, {
      method,
      headers,
      body: body === undefined ? undefined : JSON.stringify(body),
    })
  } catch {
    throw new ApiError(0, 'Cannot reach the server. Check that the backend is running.')
  }

  if (response.status === 204) {
    return undefined as T // DELETE answers 204 with no body
  }
  const data = await response.json().catch(() => null)

  if (!response.ok) {
    if (response.status === 401 && token) {
      clearToken()
      unauthorizedHandler()
    }
    const message = data?.detail || data?.title || `Request failed (${response.status})`
    throw new ApiError(response.status, message, data?.errors)
  }
  return data as T
}

export const api = {
  register: (username: string, password: string) =>
    request<UserResponse>('/auth/register', { method: 'POST', body: { username, password } }),
  login: (username: string, password: string) =>
    request<TokenResponse>('/auth/login', { method: 'POST', body: { username, password } }),

  // PokeAPI catalog (US01, US02)
  listPokemon: (page: number, size: number) =>
    request<Page<PokemonSummary>>(`/pokemon?page=${page}&size=${size}`),
  getPokemon: (idOrName: string) =>
    request<PokemonDetail>(`/pokemon/${encodeURIComponent(idOrName)}`),

  // Our local collection (US03, US04, CRUD)
  syncPokemon: (idOrName: string) =>
    request<LocalPokemon>(`/local-pokemon/sync/${encodeURIComponent(idOrName)}`, { method: 'POST' }),
  listLocalPokemon: (page: number, size: number) =>
    request<Page<LocalPokemon>>(`/local-pokemon?page=${page}&size=${size}`),
  updateLocalPokemon: (id: number, customFields: CustomFields) =>
    request<LocalPokemon>(`/local-pokemon/${id}`, { method: 'PUT', body: customFields }),
  deleteLocalPokemon: (id: number) =>
    request<void>(`/local-pokemon/${id}`, { method: 'DELETE' }),
}
