import { useEffect, useState } from 'react'
import { api, toApiError } from '../api.ts'
import type { LocalPokemon, Page } from '../types.ts'
import Pagination from '../components/Pagination.tsx'
import EditPokemonForm from '../components/EditPokemonForm.tsx'

const PAGE_SIZE = 10

// The Pokemon saved in our database: list, edit our own fields (US04) and delete.
export default function CollectionPage() {
  const [page, setPage] = useState(0)
  const [data, setData] = useState<Page<LocalPokemon> | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [editingId, setEditingId] = useState<number | null>(null)
  const [reloadCount, setReloadCount] = useState(0) // incremented to refetch the current page

  useEffect(() => {
    let ignore = false
    setError(null)
    api.listLocalPokemon(page, PAGE_SIZE)
      .then((result) => {
        if (!ignore) setData(result)
      })
      .catch((err) => {
        if (!ignore) setError(toApiError(err).message)
      })
    return () => {
      ignore = true
    }
  }, [page, reloadCount])

  function reload() {
    setReloadCount((count) => count + 1)
  }

  async function handleDelete(pokemon: LocalPokemon) {
    if (!window.confirm(`Remove ${pokemon.name} from your collection?`)) {
      return
    }
    try {
      await api.deleteLocalPokemon(pokemon.id)
      // if it was the only one on the last page, go back one page
      if (data && data.content.length === 1 && page > 0) {
        setPage(page - 1)
      } else {
        reload()
      }
    } catch (err) {
      setError(toApiError(err).message)
    }
  }

  return (
    <section>
      <div className="section-head">
        <h2>My collection</h2>
        {data && <span className="muted">{data.totalElements} saved</span>}
      </div>

      {error && <p className="error">{error}</p>}
      {!data && !error && <p className="muted">Loading your collection…</p>}

      {data && data.content.length === 0 && (
        <p className="empty">
          Your collection is empty. Go to All Pokémon and press Save on the ones you want to keep.
        </p>
      )}

      {data && data.content.length > 0 && (
        <>
          <ul className="collection">
            {data.content.map((pokemon) => (
              <li key={pokemon.id} className="collection-item">
                {pokemon.imageUrl && <img src={pokemon.imageUrl} alt="" width="88" height="88" />}

                {editingId === pokemon.id ? (
                  <EditPokemonForm
                    pokemon={pokemon}
                    onCancel={() => setEditingId(null)}
                    onSaved={() => {
                      setEditingId(null)
                      reload()
                    }}
                  />
                ) : (
                  <>
                    <div className="collection-info">
                      <h3 className="pokemon-name">
                        {pokemon.name}
                        <span className="dex-inline">#{String(pokemon.pokeapiId).padStart(3, '0')}</span>
                      </h3>
                      <dl className="facts">
                        <dt>Local name</dt>
                        <dd>{pokemon.localizedName ?? '—'}</dd>
                        <dt>Region</dt>
                        <dd>{pokemon.region ?? '—'}</dd>
                        <dt>Tags</dt>
                        <dd>
                          {pokemon.tags.length === 0
                            ? '—'
                            : pokemon.tags.map((tag) => (
                                <span key={tag} className="tag">
                                  {tag}
                                </span>
                              ))}
                        </dd>
                      </dl>
                    </div>
                    <div className="card-actions">
                      <button onClick={() => setEditingId(pokemon.id)}>Edit</button>
                      <button className="danger" onClick={() => handleDelete(pokemon)}>
                        Remove
                      </button>
                    </div>
                  </>
                )}
              </li>
            ))}
          </ul>
          <Pagination page={data.page} totalPages={data.totalPages} onChange={setPage} />
        </>
      )}
    </section>
  )
}
