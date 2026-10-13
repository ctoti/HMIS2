import { useMemo } from 'react';
import { useAuth } from 'react-oidc-context';
import { sessionFromAccessToken, type Session } from './claims';

export function useSession(): Session {
  const { user } = useAuth();
  return useMemo(() => sessionFromAccessToken(user?.access_token), [user?.access_token]);
}
