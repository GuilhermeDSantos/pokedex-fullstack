import { useMutation } from '@tanstack/react-query'
import { useState, type FormEvent } from 'react'
import { useNavigate, useSearchParams } from 'react-router'
import { ApiError } from '../../../shared/api/ApiError'
import { Button } from '../../../shared/ui/Button'
import { FormError } from '../../../shared/ui/FormError'
import { Heading } from '../../../shared/ui/Heading'
import { Stack } from '../../../shared/ui/Stack'
import { TextField } from '../../../shared/ui/TextField'
import { requireFields, type FieldErrors } from '../lib/requireFields'
import { safeReturnTo } from '../lib/returnTo'
import { useAuth } from '../useAuth'
import styles from './AuthPage.module.css'

const LABELS = { name: 'Name', email: 'Email', password: 'Password' }

type SignUpField = keyof typeof LABELS

function isSignUpField(field: string): field is SignUpField {
  return field in LABELS
}

function serverFieldErrors(error: Error | null): FieldErrors<SignUpField> {
  if (!(error instanceof ApiError)) {
    return {}
  }
  // Registering can only conflict on the email, so a 409 belongs on that field.
  if (error.status === 409) {
    return { email: error.message }
  }
  const errors: FieldErrors<SignUpField> = {}
  for (const { field, message } of error.fieldErrors) {
    if (isSignUpField(field)) {
      errors[field] = message
    }
  }
  return errors
}

export function SignUpPage() {
  const { signUp } = useAuth()
  const navigate = useNavigate()
  const [searchParams] = useSearchParams()
  const [name, setName] = useState('')
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [missing, setMissing] = useState<FieldErrors<SignUpField>>({})

  const signUpMutation = useMutation({
    mutationFn: signUp,
    onSuccess: () => navigate(safeReturnTo(searchParams.get('returnTo')), { replace: true }),
  })

  const errors = { ...serverFieldErrors(signUpMutation.error), ...missing }
  const formError = signUpMutation.error && Object.keys(errors).length === 0 ? signUpMutation.error.message : null

  const handleSubmit = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    const values = { name, email, password }
    const missingFields = requireFields(values, LABELS)
    setMissing(missingFields)
    if (Object.keys(missingFields).length > 0) {
      signUpMutation.reset()
      return
    }
    signUpMutation.mutate(values)
  }

  return (
    <section className={styles.page}>
      <Stack gap={6}>
        <Heading level={1}>Create account</Heading>
        <form onSubmit={handleSubmit} noValidate>
          <Stack>
            {formError && <FormError message={formError} />}
            <TextField
              label={LABELS.name}
              name="name"
              autoComplete="name"
              value={name}
              onChange={setName}
              error={errors.name}
            />
            <TextField
              label={LABELS.email}
              name="email"
              type="email"
              autoComplete="email"
              value={email}
              onChange={setEmail}
              error={errors.email}
            />
            <TextField
              label={LABELS.password}
              name="password"
              type="password"
              autoComplete="new-password"
              value={password}
              onChange={setPassword}
              error={errors.password}
            />
            <Button type="submit" pending={signUpMutation.isPending}>
              Create account
            </Button>
          </Stack>
        </form>
      </Stack>
    </section>
  )
}
