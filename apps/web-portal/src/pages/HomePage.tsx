import { useTranslation } from 'react-i18next';
import { Link } from 'react-router-dom';
import { hasAnyRole } from '../auth/claims';
import { useSession } from '../auth/useSession';
import { MODULES } from '../modules';

export function HomePage() {
  const { t } = useTranslation();
  const session = useSession();

  return (
    <div className="grid gap-8">
      <h1 className="font-display text-3xl font-bold">{t('home.title')}</h1>
      <section aria-labelledby="access" className="grid gap-3 rounded border border-line bg-surface p-4">
        <h2 id="access" className="font-display text-lg font-bold">{t('home.yourAccess')}</h2>
        <dl className="grid gap-3 sm:grid-cols-2">
          <div>
            <dt className="text-sm text-ink-muted">{t('home.roles')}</dt>
            <dd className="mt-1 flex flex-wrap gap-2">
              {session.roles.map((r) => (
                <span key={r} className="rounded-full bg-accent-soft px-2 py-0.5 text-sm">{t(`roles.${r}`)}</span>
              ))}
            </dd>
          </div>
          <div>
            <dt className="text-sm text-ink-muted">{t('home.facilities')}</dt>
            <dd className="mt-1 font-mono text-sm">
              {session.facilities.length ? session.facilities.join(', ') : t('home.noFacilities')}
            </dd>
          </div>
        </dl>
      </section>
      <section aria-labelledby="modules" className="grid gap-3">
        <h2 id="modules" className="font-display text-lg font-bold">{t('home.modules')}</h2>
        <ul className="grid gap-3 sm:grid-cols-2">
          {MODULES.filter((m) => hasAnyRole(session, m.roles)).map((m) => (
            <li key={m.id}>
              <Link to={m.path} className="grid gap-1 rounded border border-line bg-surface p-4 hover:border-accent focus-visible:outline-2 focus-visible:outline-accent">
                <span className="font-semibold">{t(`nav.${m.id}`)}</span>
                <span className="text-sm text-ink-muted">{t('home.available', { phase: m.phase })}</span>
              </Link>
            </li>
          ))}
        </ul>
      </section>
    </div>
  );
}
