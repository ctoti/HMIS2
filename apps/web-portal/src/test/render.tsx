import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render } from '@testing-library/react';
import type { ReactElement } from 'react';
import { I18nextProvider } from 'react-i18next';
import { MemoryRouter } from 'react-router-dom';
import { createI18n } from '../i18n';

export interface FakeAuth {
  isLoading: boolean;
  isAuthenticated: boolean;
  error?: Error | undefined;
  user?: { access_token: string } | undefined;
  signinRedirect: () => Promise<void>;
  signoutRedirect: () => Promise<void>;
}

export const fakeAuth: { current: FakeAuth } = {
  current: { isLoading: false, isAuthenticated: false, signinRedirect: async () => undefined, signoutRedirect: async () => undefined },
};

vi.mock('react-oidc-context', () => ({ useAuth: () => fakeAuth.current }));

export function tokenWith(payload: Record<string, unknown>): string {
  const encode = (value: object) => btoa(JSON.stringify(value)).replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '');
  return `${encode({ alg: 'none' })}.${encode(payload)}.sig`;
}

export function signedInAs(roles: string[], groups: string[] = []): void {
  fakeAuth.current = {
    ...fakeAuth.current,
    isLoading: false,
    isAuthenticated: true,
    user: { access_token: tokenWith({ realm_access: { roles }, groups }) },
  };
}

export function renderApp(ui: ReactElement, { route = '/' }: { route?: string } = {}) {
  const i18n = createI18n('en');
  const queryClient = new QueryClient();
  return {
    i18n,
    ...render(
      <I18nextProvider i18n={i18n}>
        <QueryClientProvider client={queryClient}>
          <MemoryRouter initialEntries={[route]}>{ui}</MemoryRouter>
        </QueryClientProvider>
      </I18nextProvider>,
    ),
  };
}
