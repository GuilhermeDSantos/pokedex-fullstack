import type { Session } from '../../features/auth/session'

export const ASH = {
  id: '00000000-0000-0000-0000-000000000001',
  email: 'ash@pallet.town',
  name: 'Ash Ketchum',
  createdAt: '2026-01-15T10:00:00Z',
}

export const ASH_SESSION: Session = {
  accessToken: 'signed.jwt.value',
  expiresAt: '2999-01-01T00:00:00Z',
  user: ASH,
}
