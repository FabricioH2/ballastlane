// The shape of the JSON our Spring Boot API returns.
// Each interface matches a Java record in web/model (PageResponse, PokemonSummaryResponse...).
// TypeScript uses them to check, while you write, that you only use fields that exist.

export interface Page<T> {
  content: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

// GET /api/pokemon (US01)
export interface PokemonSummary {
  id: number
  name: string
  spriteUrl: string | null
  category: string | null
  weightKg: number
  abilities: string[]
}

// GET /api/pokemon/{idOrName} (US02)
export interface PokemonDetail {
  id: number
  name: string
  imageUrl: string | null
  stats: Stat[]
  description: string | null
  evolutionChain: string[]
}

export interface Stat {
  name: string
  baseValue: number
}

// /api/local-pokemon (US03, US04, CRUD)
export interface LocalPokemon {
  id: number
  pokeapiId: number
  name: string
  imageUrl: string | null
  description: string | null
  localizedName: string | null
  region: string | null
  tags: string[]
  createdAt: string
  updatedAt: string
}

// Body of PUT /api/local-pokemon/{id}: only our own fields
export interface CustomFields {
  localizedName: string
  region: string
  tags: string[]
}

// POST /api/auth/login
export interface TokenResponse {
  accessToken: string
  tokenType: string
  expiresAt: string
}

// POST /api/auth/register
export interface UserResponse {
  id: number
  username: string
  createdAt: string
}

// A short message shown after pressing "Save": green if it worked, red if not.
export interface Notice {
  type: 'success' | 'error'
  text: string
}
