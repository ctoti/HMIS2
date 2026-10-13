import en from './locales/en.json';
import tpi from './locales/tpi.json';
import { createI18n, saveLanguage } from '.';

const keysOf = (obj: object, prefix = ''): string[] =>
  Object.entries(obj).flatMap(([k, v]) => (v !== null && typeof v === 'object' ? keysOf(v, `${prefix}${k}.`) : [`${prefix}${k}`]));

describe('i18n', () => {
  it('has the same keys in every language', () => {
    expect(keysOf(tpi).sort()).toEqual(keysOf(en).sort());
  });

  it('restores the saved language and sets <html lang>', async () => {
    saveLanguage('tpi');
    const i18n = createI18n();
    expect(i18n.language).toBe('tpi');
    expect(document.documentElement.lang).toBe('tpi');
    await i18n.changeLanguage('en');
    expect(document.documentElement.lang).toBe('en');
  });

  it('falls back to English for an unknown saved value or blocked storage', () => {
    localStorage.setItem('pha.language', 'fr');
    expect(createI18n().language).toBe('en');

    const spy = vi.spyOn(Storage.prototype, 'getItem').mockImplementation(() => { throw new Error('blocked'); });
    expect(createI18n().language).toBe('en');
    spy.mockRestore();
  });

  it('ignores storage failures when saving', () => {
    const spy = vi.spyOn(Storage.prototype, 'setItem').mockImplementation(() => { throw new Error('blocked'); });
    expect(() => saveLanguage('tpi')).not.toThrow();
    spy.mockRestore();
  });
});
