import { useTranslation } from 'react-i18next';
import { RequireRole } from '../auth/RequireRole';
import type { PortalModule } from '../modules';

export function ModulePage({ module }: { readonly module: PortalModule }) {
  const { t } = useTranslation();
  const name = t(`nav.${module.id}`);
  return (
    <RequireRole roles={module.roles} moduleName={name}>
      <section className="grid gap-2">
        <h1 className="font-display text-2xl font-bold">{t('module.comingTitle', { module: name })}</h1>
        <p className="text-ink-muted">{t('module.comingBody', { phase: module.phase })}</p>
      </section>
    </RequireRole>
  );
}
