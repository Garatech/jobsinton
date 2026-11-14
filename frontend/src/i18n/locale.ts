import { spanish } from './messages';
import { sampleSpanish } from './catalog';

export type Language = 'es' | 'en';
export const languageKey = 'jobsinton.language';
const locales = { es: 'es-ES', en: 'en-GB' };

export function resolveLanguage(saved: string | null, preferred: readonly string[]): Language {
  if (saved === 'es' || saved === 'en') return saved;
  for (const tag of preferred) {
    const base = tag.toLowerCase().split('-')[0];
    if (base === 'es' || base === 'en') return base;
  }
  return 'en';
}

export function translate(language: Language, source: string): string {
  if (language === 'en') return source;
  return spanish[source] ?? sampleSpanish[source] ?? source;
}

export function number(language: Language, value: number): string {
  return new Intl.NumberFormat(locales[language]).format(value);
}
export function euros(language: Language, value: number): string {
  return new Intl.NumberFormat(locales[language], { style: 'currency', currency: 'EUR', maximumFractionDigits: 0 }).format(value);
}
// Catalog dates are calendar days, not local instants.
export function date(language: Language, iso: string): string {
  return new Intl.DateTimeFormat(locales[language], { day: 'numeric', month: 'short', year: 'numeric', timeZone: 'UTC' }).format(new Date(iso + 'T00:00:00Z'));
}
