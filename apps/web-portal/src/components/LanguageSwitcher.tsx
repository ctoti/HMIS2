import { useId } from 'react';
import { useTranslation } from 'react-i18next';
import { LANGUAGES, saveLanguage, type LanguageCode } from '../i18n';

export function LanguageSwitcher() {
  const { t, i18n } = useTranslation();
  const id = useId();
  const change = (code: LanguageCode) => {
    saveLanguage(code);
    void i18n.changeLanguage(code);
  };
  return (
    <div className="flex items-center gap-2">
      <label htmlFor={id} className="text-sm text-ink-muted">{t('language')}</label>
      <select
        id={id}
        value={i18n.language}
        onChange={(e) => change(e.target.value as LanguageCode)}
        className="rounded border border-line bg-surface px-2 py-1 text-sm"
      >
        {LANGUAGES.map((l) => (
          <option key={l.code} value={l.code}>{l.label}</option>
        ))}
      </select>
    </div>
  );
}
