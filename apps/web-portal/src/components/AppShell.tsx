import { useTranslation } from 'react-i18next';
import { useAuth } from 'react-oidc-context';
import { NavLink, Outlet } from 'react-router-dom';
import { useUiStore } from '../state/ui';
import { MODULES } from '../modules';
import { LanguageSwitcher } from './LanguageSwitcher';
import { OfflineBanner } from './OfflineBanner';

const linkClass = ({ isActive }: { isActive: boolean }) =>
  `block rounded px-3 py-2 ${isActive ? 'bg-accent-soft font-semibold text-ink' : 'text-ink-muted hover:bg-surface'}`;

export function AppShell() {
  const { t } = useTranslation();
  const auth = useAuth();
  const { navOpen, toggleNav, closeNav } = useUiStore();

  return (
    <div className="min-h-screen bg-bg text-ink">
      <a href="#main" className="sr-only focus:not-sr-only focus:absolute focus:left-4 focus:top-4 focus:rounded focus:bg-surface focus:px-3 focus:py-2">
        {t('app.skipToContent')}
      </a>
      <OfflineBanner />
      <header className="flex flex-wrap items-center gap-4 border-b border-line bg-surface px-4 py-3">
        <button
          type="button"
          onClick={toggleNav}
          aria-expanded={navOpen}
          aria-controls="main-nav"
          className="rounded border border-line px-2 py-1 md:hidden"
        >
          {navOpen ? t('app.closeMenu') : t('app.openMenu')}
        </button>
        <span className="font-display text-lg font-bold">{t('app.name')}</span>
        <div className="ml-auto flex flex-wrap items-center gap-4">
          <LanguageSwitcher />
          <button
            type="button"
            onClick={() => void auth.signoutRedirect()}
            className="rounded border border-line px-3 py-1 text-sm hover:bg-bg"
          >
            {t('auth.signOut')}
          </button>
        </div>
      </header>
      <div className="mx-auto grid max-w-6xl gap-6 px-4 py-6 md:grid-cols-[14rem_1fr]">
        <nav id="main-nav" aria-label={t('app.mainNav')} className={`${navOpen ? 'block' : 'hidden'} md:block`}>
          <ul className="grid gap-1" onClick={closeNav}>
            <li><NavLink to="/" end className={linkClass}>{t('nav.home')}</NavLink></li>
            {MODULES.map((m) => (
              <li key={m.id}><NavLink to={m.path} className={linkClass}>{t(`nav.${m.id}`)}</NavLink></li>
            ))}
          </ul>
        </nav>
        <main id="main" tabIndex={-1} className="min-w-0">
          <Outlet />
        </main>
      </div>
    </div>
  );
}
