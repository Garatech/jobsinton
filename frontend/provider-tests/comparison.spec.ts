import { test, expect, type Page } from '@playwright/test';
import { PROFILE_STORAGE_KEY } from '../src/profile/storage';

const profile = { role: 'backend', technologies: ['Java'], experienceYears: 2, city: 'Madrid',
  country: 'Spain', workMode: 'hybrid', maxOnsiteDays: 1 };
const row = { id: 'adzuna:selected', title: 'Backend developer', locationLabel: 'Madrid', company: 'Example',
  excerpt: 'Modalidad: híbrida. Dos días presenciales por semana.', referral: 'https://adzuna.es/details/example',
  salary: null, matchingFacts: { mode: 'HYBRID' } };

async function saveProfile(page: Page) {
  await page.addInitScript(({ key, profile }) => localStorage.setItem(key, JSON.stringify({ version: 1, profile })),
    { key: PROFILE_STORAGE_KEY, profile });
}
async function mockSearch(page: Page, pages: number[], excerpt = row.excerpt) {
  await page.route('**/api/providers/adzuna/search', route => {
    const n = route.request().postDataJSON().page;
    pages.push(n);
    const listings = n === 1 ? Array.from({ length: 10 }, (_, i) => ({ ...row, excerpt, id: 'adzuna:' + i }))
      : [row, { ...row, id: 'adzuna:other', title: 'Another listing' }];
    return route.fulfill({ json: { source: 'Adzuna', page: n, received: listings.length, rejected: 0,
      duplicates: 0, listings, comparisonRefs: { [listings[0].id]: 'fixture-reference' } } });
  });
  await page.goto('/#opportunities');
  await page.getByRole('button', { name: 'Adzuna · Spain' }).click();
  await page.getByRole('button', { name: 'Search Adzuna' }).click();
}

test('compares the selected page-two card once and keeps its excerpt when language changes', async ({ page, request }) => {
  await saveProfile(page);
  const pages: number[] = [], comparisons: unknown[] = [];
  // Use the real engine response for a fictional two-day role; bind the fixture to our chosen result.
  const response = await request.post('/api/matching/evaluate', { data: { opportunityId: 'data-engineer', profile } });
  expect(response.ok()).toBe(true);
  const result = { ...await response.json(), opportunityId: row.id };
  await page.route('**/api/providers/adzuna/compare', route => {
    comparisons.push(route.request().postDataJSON());
    return route.fulfill({ json: result });
  });
  await mockSearch(page, pages);
  await page.getByRole('button', { name: 'Next page' }).click();
  const card = page.locator('.live-job').filter({ hasText: row.title });
  await card.getByText('Read excerpt').click();
  await expect(page.getByRole('button', { name: 'Compare with my profile' })).toHaveCount(1);
  expect(comparisons).toEqual([]);
  await card.getByRole('button', { name: 'Compare with my profile' }).click();
  await expect(card.getByText(/requires 2 office days per week; your maximum is 1/)).toBeVisible();
  await page.getByRole('button', { name: 'Español' }).click();
  await expect(page.getByText(/Página 2/)).toBeVisible();
  await expect(card.getByText(/exige 2 días presenciales por semana; tu máximo es 1/)).toBeVisible();
  await expect(card.getByText(row.excerpt, { exact: true })).toBeVisible();
  await card.getByText('Datos desconocidos o sin evaluar').click();
  await expect(card.locator('[data-criterion="residence"]')).toContainText('no especifica los países');
  await card.getByRole('button', { name: 'Ocultar comparación' }).click();
  await card.getByRole('button', { name: 'Comparar con mi perfil' }).click();
  expect(comparisons).toEqual([{ comparisonRef: 'fixture-reference', profile }]);
  expect(pages).toEqual([1, 2]);
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBe(true);
});

test('links to profile setup without posting a comparison', async ({ page }) => {
  const calls: string[] = [];
  page.on('request', r => { if (r.url().endsWith('/adzuna/compare')) calls.push(r.url()); });
  await mockSearch(page, []);
  await page.getByRole('link', { name: 'Set up a profile to compare' }).click();
  await expect(page).toHaveURL(/#profile$/);
  expect(calls).toEqual([]);
});

test('an expired reference explains recovery without refreshing or retrying automatically', async ({ page }) => {
  await saveProfile(page);
  let compares = 0;
  await page.route('**/api/providers/adzuna/compare', route => {
    compares++;
    return route.fulfill({ status: 422, json: { code: 'INVALID_COMPARISON_REF' } });
  });
  const pages: number[] = [];
  await mockSearch(page, pages);
  await page.getByRole('button', { name: 'Compare with my profile' }).click();
  await expect(page.getByRole('alert')).toHaveText('Search again to compare this listing.');
  await expect(page.getByRole('button', { name: 'Retry comparison' })).toHaveCount(0);
  expect(compares).toBe(1);
  expect(pages).toEqual([1]);
});

test('incomplete attendance stays visible as unknown on the card', async ({ page, request }) => {
  await saveProfile(page);
  const response = await request.post('/api/matching/evaluate', { data: { opportunityId: 'data-engineer', profile } });
  expect(response.ok()).toBe(true);
  const result = await response.json();
  const attendance = result.evaluation.assessments.find((a: { criterion: string }) => a.criterion === 'attendance');
  Object.assign(attendance, { outcome: 'UNKNOWN', reason: 'ATTENDANCE_UNKNOWN',
    explanation: 'The opportunity does not specify regular weekly office days.',
    evidence: { profileValues: ['1'], opportunityValues: [], limitation: 'Weekly attendance only.' } });
  result.opportunityId = 'adzuna:0';
  result.evaluation.constraintSummary = 'NEEDS_INFORMATION';
  await page.route('**/api/providers/adzuna/compare', route => route.fulfill({ json: result }));
  await mockSearch(page, [], 'Join our team.');
  await page.getByRole('button', { name: 'Compare with my profile' }).click();
  await page.getByRole('button', { name: 'Español' }).click();
  await expect(page.getByText(/no especifica los días presenciales habituales/)).toBeVisible();
});
