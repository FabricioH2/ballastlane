import { useEffect, useState } from 'react'
import { api, toApiError } from '../api.ts'
import type { Notice, PokemonDetail as PokemonDetailData } from '../types.ts'

const MAX_STAT = 255 // highest possible base stat, used to size the bars

interface PokemonDetailProps {
  name: string
  notice: Notice | null
  onBack: () => void
  onSave: (name: string) => void
}

// US02: image, stats, description and evolution chain of one Pokemon.
export default function PokemonDetail({ name, notice, onBack, onSave }: PokemonDetailProps) {
  const [pokemon, setPokemon] = useState<PokemonDetailData | null>(null)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    let ignore = false
    api.getPokemon(name)
      .then((result) => {
        if (!ignore) setPokemon(result)
      })
      .catch((err) => {
        if (!ignore) setError(toApiError(err).message)
      })
    return () => {
      ignore = true
    }
  }, [name])

  return (
    <section>
      <button className="link-button" onClick={onBack}>
        Back to all Pokémon
      </button>

      {error && <p className="error">{error}</p>}
      {!pokemon && !error && <p className="muted">Loading {name}…</p>}

      {pokemon && (
        <article className="detail">
          <div className="detail-image">
            <span className="dex-number">#{String(pokemon.id).padStart(3, '0')}</span>
            {pokemon.imageUrl && <img src={pokemon.imageUrl} alt={pokemon.name} />}
          </div>

          <div className="detail-info">
            <h2 className="pokemon-name">{pokemon.name}</h2>
            <p>{pokemon.description ?? 'No description available.'}</p>

            <h3>Base stats</h3>
            <ul className="stats">
              {pokemon.stats.map((stat) => (
                <li key={stat.name}>
                  <span className="stat-name">{stat.name.replace('-', ' ')}</span>
                  <span className="stat-value">{stat.baseValue}</span>
                  <span className="stat-bar">
                    <span style={{ width: `${(stat.baseValue / MAX_STAT) * 100}%` }} />
                  </span>
                </li>
              ))}
            </ul>

            <h3>Evolution chain</h3>
            <ol className="evolution">
              {pokemon.evolutionChain.map((step) => (
                <li key={step} className={step === pokemon.name ? 'current' : undefined}>
                  {step}
                </li>
              ))}
            </ol>

            {notice && <p className={notice.type}>{notice.text}</p>}
            <button className="primary" onClick={() => onSave(pokemon.name)}>
              Save to my collection
            </button>
          </div>
        </article>
      )}
    </section>
  )
}
