import { useState, type FormEvent } from 'react'
import { ApiError } from '../../../shared/api/ApiError'
import { Button } from '../../../shared/ui/Button'
import { FormError } from '../../../shared/ui/FormError'
import { Stack } from '../../../shared/ui/Stack'
import { TextField } from '../../../shared/ui/TextField'
import type { LocalAttributes } from '../api/pokemonApi'
import { useUpdateLocalPokemon } from '../hooks/useUpdateLocalPokemon'

type LocalFormProps = {
  pokedexNumber: number
  local: LocalAttributes
  onDone: () => void
}

const FIELDS = ['localizedName', 'region', 'tags'] as const

type LocalField = (typeof FIELDS)[number]

function isLocalField(field: string): field is LocalField {
  return (FIELDS as readonly string[]).includes(field)
}

function fieldErrors(error: Error | null): Partial<Record<LocalField, string>> {
  const errors: Partial<Record<LocalField, string>> = {}
  if (error instanceof ApiError) {
    for (const { field, message } of error.fieldErrors) {
      if (isLocalField(field)) {
        errors[field] = message
      }
    }
  }
  return errors
}

function toTags(text: string): string[] {
  return text
    .split(',')
    .map((tag) => tag.trim())
    .filter((tag) => tag.length > 0)
}

export function LocalForm({ pokedexNumber, local, onDone }: LocalFormProps) {
  const [localizedName, setLocalizedName] = useState(local.localizedName ?? '')
  const [region, setRegion] = useState(local.region ?? '')
  const [tags, setTags] = useState(local.tags.join(', '))
  const update = useUpdateLocalPokemon(pokedexNumber)
  const errors = fieldErrors(update.error)
  const formError = update.error && Object.keys(errors).length === 0 ? update.error.message : null

  const handleSubmit = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    update.mutate({ localizedName, region, tags: toTags(tags) }, { onSuccess: onDone })
  }

  return (
    <form onSubmit={handleSubmit} noValidate aria-label="Edit our fields">
      <Stack>
        {formError && <FormError message={formError} />}
        <TextField
          label="Localized name"
          name="localizedName"
          value={localizedName}
          onChange={setLocalizedName}
          error={errors.localizedName}
        />
        <TextField label="Region" name="region" value={region} onChange={setRegion} error={errors.region} />
        <TextField
          label="Tags"
          name="tags"
          value={tags}
          onChange={setTags}
          hint="Separate tags with commas"
          error={errors.tags}
        />
        <Button type="submit" pending={update.isPending}>
          Save
        </Button>
        <Button onClick={onDone}>Cancel</Button>
      </Stack>
    </form>
  )
}
