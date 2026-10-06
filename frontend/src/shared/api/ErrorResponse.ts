export type FieldError = {
  field: string
  message: string
}

export type ErrorResponse = {
  code: string
  message: string
  fieldErrors: FieldError[]
}
