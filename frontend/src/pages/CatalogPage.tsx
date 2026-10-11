import { useEffect, useState } from 'react'
import { api, toApiError } from '../api.ts'
import type { Notice, Page, PokemonSummary } from '../types.ts'
import Pagination from '../components/Pagination.tsx'
import PokemonDetail from '../components/PokemonDetail.tsx'

const PAGE_SIZE = 12

// US01 (list with pagination) + US02 (detail) + US03 (save to my collection).
export default function CatalogPage() {
  const [page, setPage] = useState(0)
  const [data, setData] = useState<Page<PokemonSummary> | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [selected, setSelected] = useState<string | null>(null) // name of the Pokemon whose detail is open
  const [notice, setNotice] = useState<Notice | null>(null)

  // Runs on the first render and every time "page" changes.
  useEffect(() => {
    let ignore = false // if the user changes page before the answer arrives, ignore the old answer
    setLoading(true)
    setError(null)
    api.listPokemon(page, PAGE_SIZE)
      .then((result) => {
        if (!ignore) setData(result)
      })
      .catch((err) => {
        if (!ignore) setError(toApiError(err).message)
      })
      .finally(() => {
        if (!ignore) setLoading(false)
      })
    return () => {
      ignore = true
    }
  }, [page])

  async function saveToCollection(name: string) {
    setNotice(null)
    try {
      await api.syncPokemon(name)
      setNotice({ type: 'success', text: `${name} was added to your collection.` })
    } catch (err) {
      const apiError = toApiError(err)
      const text = apiError.status === 409 ? `${name} is already in your collection.` : apiError.message
      setNotice({ type: 'error', text })
    }
  }

  if (selected) {
    return (
      <PokemonDetail
        name={selected}
        notice={notice}
        onBack={() => {
          setSelected(null)
          setNotice(null)
        }}
        onSave={saveToCollection}
      />
    )
  }

  return (
    <section>
      <div className="section-head">
        <h2>All Pokémon</h2>
        {data && <span className="muted">{data.totalElements} in the PokeAPI</span>}
      </div>

      {notice && <p className={notice.type}>{notice.text}</p>}
      {error && <p className="error">{error}</p>}
      {loading && <p className="muted">Loading Pokémon…</p>}

      {!loading && data && (
        <>
          <ul className="grid">
            {data.content.map((pokemon) => (
              <li key={pokemon.id} className="card">
                <span className="dex-number">#{String(pokemon.id).padStart(3, '0')}</span>
                {pokemon.spriteUrl && <img src={pokemon.spriteUrl} alt="" width="96" height="96" />}
                <h3 className="pokemon-name">{pokemon.name}</h3>
                <p className="muted">{pokemon.category ?? 'Unknown category'}</p>
                <dl className="facts">
                  <dt>Weight</dt>
                  <dd>{pokemon.weightKg} kg</dd>
                  <dt>Abilities</dt>
                  <dd>{pokemon.abilities.join(', ')}</dd>
                </dl>
                <div className="card-actions">
                  <button onClick={() => setSelected(pokemon.name)}>View details</button>
                  <button className="primary" onClick={() => saveToCollection(pokemon.name)}>
                    Save
                  </button>
                </div>
              </li>
            ))}
          </ul>
          <Pagination page={data.page} totalPages={data.totalPages} onChange={setPage} />
        </>
      )}
    </section>
  )
}
