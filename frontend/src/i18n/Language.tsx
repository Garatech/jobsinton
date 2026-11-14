import { createContext, useContext, useLayoutEffect, useMemo, useState, type ReactNode } from 'react';
import { date, euros, languageKey, number, resolveLanguage, translate, type Language } from './locale';

function initialLanguage() {
  let saved: string | null = null;
  try { saved = localStorage.getItem(languageKey); } catch { /* Private storage can be unavailable. */ }
  return resolveLanguage(saved, navigator.languages?.length ? navigator.languages : [navigator.language]);
}
function labels(language: Language) {
  return {
    language, t: (source: string) => translate(language, source),
    number: (value: number) => number(language, value), euros: (value: number) => euros(language, value),
    date: (iso: string) => date(language, iso),
  };
}
const Context = createContext<ReturnType<typeof labels> & { choose: (lang: Language) => void } | null>(null);
export function LanguageProvider({ children }: { children: ReactNode }) {
  const [language, setLanguage] = useState(initialLanguage);
  useLayoutEffect(() => {
    document.documentElement.lang = language;
    document.querySelector('meta[name="description"]')?.setAttribute('content', language === 'es'
      ? 'JobSinton: ofertas de empleo que encajan con tu perfil y tus preferencias.'
      : 'JobSinton — job opportunities in tune with you. A job discovery application in early development.');
  }, [language]);
  const value = useMemo(() => ({ ...labels(language), choose(next: Language) {
    setLanguage(next);
    try { localStorage.setItem(languageKey, next); } catch { /* The in-memory choice still applies. */ }
  } }), [language]);
  return <Context.Provider value={value}>{children}</Context.Provider>;
}
export function useLanguage() {
  const value = useContext(Context);
  if (!value) throw new Error('LanguageProvider is required.');
  return value;
}
export function LanguageSwitch() {
  const { language, choose, t } = useLanguage();
  return <div className="language-switch" role="group" aria-label={t('Language')}>
    <button type="button" lang="es" aria-pressed={language === 'es'} onClick={() => choose('es')}>Español</button>
    <span aria-hidden="true">|</span>
    <button type="button" lang="en" aria-pressed={language === 'en'} onClick={() => choose('en')}>English</button>
  </div>;
}
