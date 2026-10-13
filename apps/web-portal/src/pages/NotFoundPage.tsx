import { useTranslation } from 'react-i18next';
import { Link } from 'react-router-dom';

export function NotFoundPage() {
  const { t } = useTranslation();
  return (
    <section className="grid gap-3">
      <h1 className="font-display text-2xl font-bold">{t('notFound.title')}</h1>
      <Link to="/" className="text-accent underline">{t('notFound.back')}</Link>
    </section>
  );
}
