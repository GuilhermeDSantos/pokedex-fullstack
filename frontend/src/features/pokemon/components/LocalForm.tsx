import { useState, type FormEvent } from 'react'
import { Button } from '../../../shared/ui/Button'
import { Stack } from '../../../shared/ui/Stack'
import { TextField } from '../../../shared/ui/TextField'
import type { LocalAttributes } from '../api/pokemonApi'
import { useUpdateLocalPokemon } from '../hooks/useUpdateLocalPokemon'

type LocalFormProps = {
  pokedexNumber: number
  local: LocalAttributes
  onDone: () => void
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

  const handleSubmit = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    update.mutate({ localizedName, region, tags: toTags(tags) }, { onSuccess: onDone })
  }

  return (
    <form onSubmit={handleSubmit} noValidate aria-label="Edit our fields">
      <Stack>
        <TextField label="Localized name" name="localizedName" value={localizedName} onChange={setLocalizedName} />
        <TextField label="Region" name="region" value={region} onChange={setRegion} />
        <TextField label="Tags" name="tags" value={tags} onChange={setTags} hint="Separate tags with commas" />
        <Button type="submit" pending={update.isPending}>
          Save
        </Button>
        <Button onClick={onDone}>Cancel</Button>
      </Stack>
    </form>
  )
}
