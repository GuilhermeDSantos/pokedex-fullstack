import { useMutation } from '@tanstack/react-query'
import { useState, type FormEvent } from 'react'
import { useNavigate, useSearchParams } from 'react-router'
import { Button } from '../../../shared/ui/Button'
import { FormError } from '../../../shared/ui/FormError'
import { Heading } from '../../../shared/ui/Heading'
import { Stack } from '../../../shared/ui/Stack'
import { TextField } from '../../../shared/ui/TextField'
import { requireFields, type FieldErrors } from '../lib/requireFields'
import { safeReturnTo } from '../lib/returnTo'
import { useAuth } from '../useAuth'
import styles from './AuthPage.module.css'

const LABELS = { email: 'Email', password: 'Password' }

export function SignInPage() {
  const { signIn } = useAuth()
  const navigate = useNavigate()
  const [searchParams] = useSearchParams()
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [fieldErrors, setFieldErrors] = useState<FieldErrors<'email' | 'password'>>({})

  const signInMutation = useMutation({
    mutationFn: signIn,
    onSuccess: () => navigate(safeReturnTo(searchParams.get('returnTo')), { replace: true }),
  })

  const handleSubmit = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    const missing = requireFields({ email, password }, LABELS)
    setFieldErrors(missing)
    if (Object.keys(missing).length === 0) {
      signInMutation.mutate({ email, password })
    }
  }

  return (
    <section className={styles.page}>
      <Stack gap={6}>
        <Heading level={1}>Sign in</Heading>
        <form onSubmit={handleSubmit} noValidate>
          <Stack>
            {signInMutation.error && <FormError message={signInMutation.error.message} />}
            <TextField
              label={LABELS.email}
              name="email"
              type="email"
              autoComplete="email"
              value={email}
              onChange={setEmail}
              error={fieldErrors.email}
            />
            <TextField
              label={LABELS.password}
              name="password"
              type="password"
              autoComplete="current-password"
              value={password}
              onChange={setPassword}
              error={fieldErrors.password}
            />
            <Button type="submit" pending={signInMutation.isPending}>
              Sign in
            </Button>
          </Stack>
        </form>
      </Stack>
    </section>
  )
}
