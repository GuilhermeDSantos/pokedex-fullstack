export type FieldErrors<Field extends string> = Partial<Record<Field, string>>

// Required-ness only: format and length rules belong to the backend (D-028), which reports them as fieldErrors.
export function requireFields<Field extends string>(
  values: Record<Field, string>,
  labels: Record<Field, string>,
): FieldErrors<Field> {
  const errors: FieldErrors<Field> = {}
  for (const field of Object.keys(values) as Field[]) {
    if (values[field].trim() === '') {
      errors[field] = `${labels[field]} is required`
    }
  }
  return errors
}
