import i18next, { type i18n } from 'i18next';
import { initReactI18next } from 'react-i18next';
import en from './locales/en.json';
import tpi from './locales/tpi.json';

export const LANGUAGES = [
  { code: 'en', label: 'English' },
  { code: 'tpi', label: 'Tok Pisin' },
] as const;

export type LanguageCode = (typeof LANGUAGES)[number]['code'];

const STORAGE_KEY = 'pha.language';

const isLanguage = (value: unknown): value is LanguageCode => LANGUAGES.some((l) => l.code === value);

function readSavedLanguage(): LanguageCode {
  try {
    const saved = localStorage.getItem(STORAGE_KEY);
    return isLanguage(saved) ? saved : 'en';
  } catch {
    return 'en';
  }
}

export function saveLanguage(code: LanguageCode): void {
  try {
    localStorage.setItem(STORAGE_KEY, code);
  } catch {
    // Storage can be unavailable (private mode); the choice then lasts for this visit only.
  }
}

export function createI18n(language: LanguageCode = readSavedLanguage()): i18n {
  const instance = i18next.createInstance();
  void instance.use(initReactI18next).init({
    resources: { en: { translation: en }, tpi: { translation: tpi } },
    lng: language,
    fallbackLng: 'en',
    interpolation: { escapeValue: false },
    initAsync: false,
  });
  document.documentElement.lang = language;
  instance.on('languageChanged', (lng) => {
    document.documentElement.lang = lng;
  });
  return instance;
}
