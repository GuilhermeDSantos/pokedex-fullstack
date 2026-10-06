import { useMutation } from '@tanstack/react-query'
import { useState, type FormEvent } from 'react'
import { useNavigate, useSearchParams } from 'react-router'
import { ApiError } from '../../../shared/api/ApiError'
import { Button } from '../../../shared/ui/Button'
import { Heading } from '../../../shared/ui/Heading'
import { Stack } from '../../../shared/ui/Stack'
import { TextField } from '../../../shared/ui/TextField'
import type { FieldErrors } from '../lib/requireFields'
import { safeReturnTo } from '../lib/returnTo'
import { useAuth } from '../useAuth'
import styles from './AuthPage.module.css'

const LABELS = { name: 'Name', email: 'Email', password: 'Password' }

type SignUpField = keyof typeof LABELS

// Registering can only conflict on the email, so a 409 belongs on that field.
function serverFieldErrors(error: Error | null): FieldErrors<SignUpField> {
  if (error instanceof ApiError && error.status === 409) {
    return { email: error.message }
  }
  return {}
}

export function SignUpPage() {
  const { signUp } = useAuth()
  const navigate = useNavigate()
  const [searchParams] = useSearchParams()
  const [name, setName] = useState('')
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')

  const signUpMutation = useMutation({
    mutationFn: signUp,
    onSuccess: () => navigate(safeReturnTo(searchParams.get('returnTo')), { replace: true }),
  })

  const errors = serverFieldErrors(signUpMutation.error)

  const handleSubmit = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    signUpMutation.mutate({ name, email, password })
  }

  return (
    <section className={styles.page}>
      <Stack gap={6}>
        <Heading level={1}>Create account</Heading>
        <form onSubmit={handleSubmit} noValidate>
          <Stack>
            <TextField label={LABELS.name} name="name" autoComplete="name" value={name} onChange={setName} />
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
