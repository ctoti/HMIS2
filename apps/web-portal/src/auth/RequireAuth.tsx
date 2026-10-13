import type { ReactNode } from 'react';
import { useTranslation } from 'react-i18next';
import { useAuth } from 'react-oidc-context';

export function RequireAuth({ children }: { readonly children: ReactNode }) {
  const auth = useAuth();
  const { t } = useTranslation();

  if (auth.isLoading) {
    return <p role="status" className="p-8 text-ink-muted">{t('auth.checking')}</p>;
  }
  if (auth.isAuthenticated) {
    return <>{children}</>;
  }
  return (
    <main id="main" className="mx-auto grid max-w-xl gap-4 px-4 py-16">
      <h1 className="font-display text-3xl font-bold">{t('auth.signInTitle')}</h1>
      <p className="text-ink-muted">{t('auth.signInBody')}</p>
      {auth.error && (
        <p role="alert" className="rounded border border-crit bg-crit-soft p-3 text-crit">
          {t('auth.error', { message: auth.error.message })}
        </p>
      )}
      <button
        type="button"
        onClick={() => void auth.signinRedirect()}
        className="justify-self-start rounded bg-accent px-4 py-2 font-semibold text-on-accent focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-accent"
      >
        {t('auth.signIn')}
      </button>
    </main>
  );
}
