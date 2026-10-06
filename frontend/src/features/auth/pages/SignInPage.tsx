import { useMutation } from '@tanstack/react-query'
import { useState, type FormEvent } from 'react'
import { useNavigate, useSearchParams } from 'react-router'
import { Button } from '../../../shared/ui/Button'
import { FormError } from '../../../shared/ui/FormError'
import { Heading } from '../../../shared/ui/Heading'
import { Stack } from '../../../shared/ui/Stack'
import { TextField } from '../../../shared/ui/TextField'
import { safeReturnTo } from '../lib/returnTo'
import { useAuth } from '../useAuth'
import styles from './AuthPage.module.css'

export function SignInPage() {
  const { signIn } = useAuth()
  const navigate = useNavigate()
  const [searchParams] = useSearchParams()
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')

  const signInMutation = useMutation({
    mutationFn: signIn,
    onSuccess: () => navigate(safeReturnTo(searchParams.get('returnTo')), { replace: true }),
  })

  const handleSubmit = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    signInMutation.mutate({ email, password })
  }

  return (
    <section className={styles.page}>
      <Stack gap={6}>
        <Heading level={1}>Sign in</Heading>
        <form onSubmit={handleSubmit} noValidate>
          <Stack>
            {signInMutation.error && <FormError message={signInMutation.error.message} />}
            <TextField label="Email" name="email" type="email" autoComplete="email" value={email} onChange={setEmail} />
            <TextField
              label="Password"
              name="password"
              type="password"
              autoComplete="current-password"
              value={password}
              onChange={setPassword}
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
