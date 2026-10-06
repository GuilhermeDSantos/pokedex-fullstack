import { useMutation } from '@tanstack/react-query'
import { useState, type FormEvent } from 'react'
import { useNavigate, useSearchParams } from 'react-router'
import { Button } from '../../../shared/ui/Button'
import { Heading } from '../../../shared/ui/Heading'
import { Stack } from '../../../shared/ui/Stack'
import { TextField } from '../../../shared/ui/TextField'
import { safeReturnTo } from '../lib/returnTo'
import { useAuth } from '../useAuth'
import styles from './AuthPage.module.css'

const LABELS = { name: 'Name', email: 'Email', password: 'Password' }

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
