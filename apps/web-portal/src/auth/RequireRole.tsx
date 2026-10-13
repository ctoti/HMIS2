import type { ReactNode } from 'react';
import { useTranslation } from 'react-i18next';
import { hasAnyRole, type Role } from './claims';
import { useSession } from './useSession';

interface Props {
  readonly roles: readonly Role[];
  readonly moduleName: string;
  readonly children: ReactNode;
}

export function RequireRole({ roles, moduleName, children }: Props) {
  const session = useSession();
  const { t } = useTranslation();
  if (hasAnyRole(session, roles)) {
    return <>{children}</>;
  }
  return (
    <section className="grid gap-2">
      <h1 className="font-display text-2xl font-bold">{t('auth.noAccessTitle', { module: moduleName })}</h1>
      <p className="text-ink-muted">{t('auth.noAccessBody')}</p>
    </section>
  );
}
