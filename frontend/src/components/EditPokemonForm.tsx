import { useState } from 'react'
import type { SubmitEvent } from 'react'
import { api, toApiError } from '../api.ts'
import type { LocalPokemon } from '../types.ts'

interface EditPokemonFormProps {
  pokemon: LocalPokemon
  onCancel: () => void
  onSaved: () => void
}

// US04: edit our own fields. The official data (name, image...) is not in the form, so it cannot change.
export default function EditPokemonForm({ pokemon, onCancel, onSaved }: EditPokemonFormProps) {
  const [localizedName, setLocalizedName] = useState(pokemon.localizedName ?? '')
  const [region, setRegion] = useState(pokemon.region ?? '')
  const [tags, setTags] = useState(pokemon.tags.join(', ')) // edited as "electric, starter"
  const [error, setError] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)

  async function handleSubmit(event: SubmitEvent<HTMLFormElement>) {
    event.preventDefault()
    setBusy(true)
    setError(null)
    try {
      await api.updateLocalPokemon(pokemon.id, {
        localizedName,
        region,
        tags: tags.split(',').map((tag) => tag.trim()).filter((tag) => tag !== ''),
      })
      onSaved()
    } catch (err) {
      // a 400 lists the invalid fields: show them all in one line
      const apiError = toApiError(err)
      const fieldMessages = Object.entries(apiError.fieldErrors).map(([field, msg]) => `${field}: ${msg}`)
      setError(fieldMessages.length > 0 ? fieldMessages.join('; ') : apiError.message)
    } finally {
      setBusy(false)
    }
  }

  return (
    <form className="edit-form" onSubmit={handleSubmit}>
      <h3 className="pokemon-name">{pokemon.name}</h3>
      <label>
        Local name
        <input value={localizedName} onChange={(e) => setLocalizedName(e.target.value)} maxLength={100} />
      </label>
      <label>
        Region
        <input value={region} onChange={(e) => setRegion(e.target.value)} maxLength={100} />
      </label>
      <label>
        Tags, separated by commas
        <input value={tags} onChange={(e) => setTags(e.target.value)} placeholder="electric, starter" />
      </label>
      {error && <p className="error">{error}</p>}
      <div className="card-actions">
        <button type="button" onClick={onCancel}>
          Cancel
        </button>
        <button className="primary" type="submit" disabled={busy}>
          Save changes
        </button>
      </div>
    </form>
  )
}
