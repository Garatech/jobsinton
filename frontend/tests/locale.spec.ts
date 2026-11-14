import { test, expect } from '@playwright/test';
import { resolveLanguage, translate, number, euros, date } from '../src/i18n/locale';

test('saved preference wins; regional browser tags respect preference order', () => {
  expect(resolveLanguage('en', ['es-ES'])).toBe('en');
  expect(resolveLanguage('invalid', ['fr', 'es-AR', 'en'])).toBe('es');
  expect(resolveLanguage(null, ['en-US', 'es'])).toBe('en');
  expect(resolveLanguage(null, [])).toBe('en');
});
test('owned copy and localized formats use the selected language', () => {
  expect(translate('es','Next page')).toBe('Siguiente página');
  expect(translate('en','Next page')).toBe('Next page');
  expect(number('es', 42500)).toContain('42.500');
  expect(euros('en', 42500)).toBe('€42,500');
  expect(date('es', '2026-01-02')).toMatch(/2.*ene.*2026/);
});
