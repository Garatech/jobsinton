import { expect, test, type Page } from '@playwright/test';
import { parseRankedOrder } from '../src/api/ranking';
import { PROFILE_STORAGE_KEY } from '../src/profile/storage';

const endpoint = '/api/matching/rank';
const profile = { role: 'backend', technologies: ['Java', 'Spring Boot'], experienceYears: 3,
  country: 'Spain', city: 'Madrid', workMode: 'remote', maxOnsiteDays: null };
const sort = (page: Page) => page.getByLabel('Sort by', { exact: true });
const cards = (page: Page) => page.locator('.results [data-job]');
const ready = (page: Page) => page.getByText('Ordered by compatibility.', { exact: true });
const ids = (page: Page) => cards(page).evaluateAll(nodes => nodes.map(node => node.getAttribute('data-job')));
async function seed(page: Page, saved: object = profile) {
  await page.addInitScript(({ key, saved }) => {
    if (!sessionStorage.getItem('ranking-test-seeded')) {
      localStorage.setItem(key, JSON.stringify({ version: 1, profile: saved }));
      sessionStorage.setItem('ranking-test-seeded', '1');
    }
  }, { key: PROFILE_STORAGE_KEY, saved });
}

test('one real batch orders only filtered offers and survives layout, shortlist and navigation', async ({ page }) => {
  await seed(page);
  const batches: { opportunityIds: string[]; profile: object }[] = [];
  page.on('request', r => { if (r.url().endsWith(endpoint)) batches.push(r.postDataJSON()); });
  await page.goto('/#opportunities');
  await expect(sort(page)).toHaveValue('newest');
  await expect(cards(page).first()).toHaveAttribute('data-job', 'data-engineer');
  await sort(page).selectOption('salary');
  await expect(cards(page).first()).toHaveAttribute('data-job', 'platform-engineer');
  expect(batches).toHaveLength(0);
  await sort(page).selectOption('compatibility');
  await expect(ready(page)).toHaveCount(1);
  await expect(cards(page).first()).toHaveAttribute('data-job', 'backend-java');
  expect(batches).toHaveLength(1);
  expect(batches[0].opportunityIds).toHaveLength(6);
  expect(batches[0].profile).toEqual(profile);

  await page.getByLabel('Remote', { exact: true }).check();
  await expect(ready(page)).toHaveCount(1);
  await expect(cards(page)).toHaveCount(3);
  expect(await ids(page)).toEqual(['backend-java', 'frontend-react', 'platform-engineer']);
  expect(batches[1].opportunityIds.slice().sort()).toEqual(['backend-java', 'frontend-react', 'platform-engineer']);
  await page.getByRole('button', { name: 'Card view' }).click();
  await page.getByRole('button', { name: 'Save Backend Developer', exact: true }).click();
  await expect(page.locator('.saved-count')).toHaveText('1');
  await page.getByRole('link', { name: 'Backend Developer', exact: true }).click();
  await expect(page.locator('[data-comparison-for="backend-java"]')).toBeVisible();
  expect(batches).toHaveLength(2);
  await page.goBack();
  await expect(ready(page)).toHaveCount(1);
  await expect(sort(page)).toHaveValue('compatibility');
  await expect(page.getByRole('button', { name: 'Card view' })).toHaveAttribute('aria-pressed', 'true');
  await expect(page.getByLabel('Remote', { exact: true })).toBeChecked();
  await page.reload();
  await expect(ready(page)).toHaveCount(1);
  expect(await ids(page)).toEqual(['backend-java', 'frontend-react', 'platform-engineer']);
});

test('no saved profile restores Newest in the control and URL without transmitting a request', async ({ page }) => {
  let count = 0;
  page.on('request', r => { if (r.url().endsWith(endpoint)) count++; });
  await page.goto('/#opportunities?sort=compatibility&mode=Remote');
  await expect(sort(page)).toHaveValue('newest');
  await expect(page.getByRole('link', { name: 'Save a profile' })).toBeVisible();
  await expect(page).not.toHaveURL(/sort=compatibility/);
  await expect(cards(page)).toHaveCount(3);
  await sort(page).selectOption('salary');
  await sort(page).selectOption('compatibility');
  await expect(sort(page)).toHaveValue('newest');
  await expect(page.getByRole('link', { name: 'Save a profile' })).toBeVisible();
  expect(count).toBe(0);
});

test('an empty filtered set needs no batch; clearing the filters ranks again', async ({ page }) => {
  await seed(page);
  let count = 0;
  page.on('request', r => { if (r.url().endsWith(endpoint)) count++; });
  await page.goto('/#opportunities?q=no-such-firm&sort=compatibility');
  await expect(page.getByText('No opportunities found.')).toBeVisible();
  await expect(sort(page)).toHaveValue('compatibility');
  expect(count).toBe(0);
  await page.getByRole('button', { name: 'Clear filters', exact: true }).click();
  await expect(ready(page)).toHaveCount(1);
  expect(count).toBe(1);
});

test('a failed or malformed batch falls back to Newest and retries with the current filters', async ({ page }) => {
  await seed(page);
  let attempt = 0;
  const sent: string[][] = [];
  await page.route('**' + endpoint, async route => {
    const requestIds = route.request().postDataJSON().opportunityIds as string[];
    sent.push(requestIds);
    if (++attempt === 1) return route.fulfill({ status: 503, body: '' });
    if (attempt === 2) return route.fulfill({ json: { apiVersion: 'ranking-api-v1', matchingVersion: 'matching-v1.0',
      rankingVersion: 'ranking-v1.0', ranked: ['wrong-id'], unavailable: [] } });
    return route.continue();
  });
  await page.goto('/#opportunities?sort=compatibility&view=cards');
  const retry = page.getByRole('button', { name: 'Retry ranking' });
  await expect(retry).toBeVisible();
  await expect(sort(page)).toHaveValue('newest');
  await expect(page).not.toHaveURL(/sort=compatibility/);
  await expect(cards(page).first()).toHaveAttribute('data-job', 'data-engineer');
  await page.getByLabel('Remote', { exact: true }).check();
  await expect(cards(page)).toHaveCount(3);
  await retry.focus(); await page.keyboard.press('Enter');
  await expect(retry).toBeVisible();
  await expect(sort(page)).toHaveValue('newest');
  await expect(cards(page)).toHaveCount(3);
  await retry.click();
  await expect(ready(page)).toHaveCount(1);
  expect(sent.map(v => v.length)).toEqual([6, 3, 3]);
  await expect(page.locator('.results .jobs')).toHaveClass(/cards/);
  await expect(page).toHaveURL(/sort=compatibility/);
});

test('a missing comparison stays visible at the end and can be saved', async ({ page }) => {
  await seed(page);
  await page.route('**' + endpoint, route => route.fulfill({ json: {
    apiVersion: 'ranking-api-v1', matchingVersion: 'matching-v1.0', rankingVersion: 'ranking-v1.0',
    ranked: ['platform-engineer', 'frontend-react'], unavailable: ['backend-java'],
  } }));
  await page.goto('/#opportunities?mode=Remote&sort=compatibility');
  await expect(ready(page)).toHaveCount(1);
  expect(await ids(page)).toEqual(['platform-engineer', 'frontend-react', 'backend-java']);
  await expect(cards(page).last()).toContainText('Comparison unavailable');
  await cards(page).last().getByRole('button', { name: 'Save Backend Developer' }).click();
  await expect(page.locator('.saved-count')).toHaveText('1');
});

for (const lateStatus of [200, 503]) {
  test('ignores superseded filter response, HTTP ' + lateStatus, async ({ page }) => {
    await seed(page);
    // Hold delivery in the browser: a superseded transport can resolve even after abort.
    // Synchronize on delivery to fetch, not on streaming the body of an HTTP error.
    await page.addInitScript(status => {
      const release = new Promise<void>(resolve => window.addEventListener('release-old-ranking', () => resolve(), { once: true }));
      const original = window.fetch.bind(window);
      window.fetch = async (input, init) => {
        if (!String(input).endsWith('/api/matching/rank') || typeof init?.body !== 'string'
          || JSON.parse(init.body).opportunityIds.length !== 6) return original(input, init);
        const response = await original(input, { ...init, signal: undefined });
        const body = await response.text();
        document.documentElement.dataset.rankingCaptured = 'true';
        await release;
        document.documentElement.dataset.rankingDelivered = String(status);
        return new Response(status === 200 ? body : '', { status, headers: { 'Content-Type': 'application/json' } });
      };
    }, lateStatus);
    await page.goto('/#opportunities?sort=compatibility');
    await page.waitForFunction(() => document.documentElement.dataset.rankingCaptured === 'true');
    await expect(page.getByText(/Ranking opportunities/)).toBeVisible();
    await page.getByLabel('Remote', { exact: true }).check();
    await expect(ready(page)).toHaveCount(1);
    await page.evaluate(() => window.dispatchEvent(new Event('release-old-ranking')));
    await page.waitForFunction(status => document.documentElement.dataset.rankingDelivered === String(status), lateStatus);
    await page.evaluate(() => new Promise<void>(done => requestAnimationFrame(() => requestAnimationFrame(() => done()))));
    await expect(sort(page)).toHaveValue('compatibility');
    expect(await ids(page)).toEqual(['backend-java', 'frontend-react', 'platform-engineer']);
    await expect(page.getByRole('button', { name: 'Retry ranking' })).toHaveCount(0);
  });
}

test('deadline leaves ordinary results and an explicit retry', async ({ page }) => {
  await seed(page);
  await page.clock.install();
  let captured!: () => void, release!: () => void;
  const started = new Promise<void>(resolve => { captured = resolve; });
  const pending = new Promise<void>(resolve => { release = resolve; });
  await page.route('**' + endpoint, async route => {
    captured(); await pending;
    await route.abort().catch(() => {});
  });
  await page.goto('/#opportunities?sort=compatibility');
  await started;
  await page.clock.fastForward(8001);
  await expect(page.getByRole('button', { name: 'Retry ranking' })).toBeVisible();
  await expect(sort(page)).toHaveValue('newest');
  await expect(cards(page)).toHaveCount(6);
  release(); await page.unrouteAll({ behavior: 'wait' });
  await page.getByRole('button', { name: 'Retry ranking' }).click();
  await expect(ready(page)).toHaveCount(1);
});

test('only saved changes alter the rank and clearing the profile restores ordinary search', async ({ page }) => {
  await seed(page, { ...profile, role: 'data', technologies: ['Python', 'SQL'], city: 'Valencia', workMode: 'hybrid', maxOnsiteDays: 2 });
  const search = () => page.evaluate(() => { location.hash = '#opportunities?sort=compatibility'; });
  await page.goto('/#opportunities?sort=compatibility');
  await expect(ready(page)).toHaveCount(1);
  await expect(cards(page).first()).toHaveAttribute('data-job', 'data-engineer');
  await page.locator('.site-nav a[href="#profile"]').click();
  await page.getByLabel('Maximum on-site days per week').selectOption('1');
  await search();
  await expect(ready(page)).toHaveCount(1);
  await expect(cards(page).first()).toHaveAttribute('data-job', 'data-engineer');
  await page.locator('.site-nav a[href="#profile"]').click();
  await page.getByRole('button', { name: 'Save profile', exact: true }).click();
  await search();
  await expect(ready(page)).toHaveCount(1);
  const ordered = await ids(page);
  expect(ordered.indexOf('backend-java')).toBeLessThan(ordered.indexOf('data-engineer'));
  await page.locator('.site-nav a[href="#profile"]').click();
  page.once('dialog', dialog => dialog.accept());
  await page.getByRole('button', { name: 'Clear saved profile', exact: true }).click();
  await search();
  await expect(sort(page)).toHaveValue('newest');
  await expect(page.getByRole('link', { name: 'Save a profile' })).toBeVisible();
});

test('response membership and versions must match the batch exactly', () => {
  const good = { apiVersion: 'ranking-api-v1', rankingVersion: 'ranking-v1.0', matchingVersion: 'matching-v1.0',
    ranked: ['a'], unavailable: ['b'] };
  const parse = (payload: unknown) => parseRankedOrder(payload, ['a', 'b']);
  expect(parse(good)).toEqual({ ranked: ['a'], unavailable: ['b'] });
  for (const bad of [null, { ...good, ranked: [] }, { ...good, ranked: ['b'] }, { ...good, unavailable: ['extra'] },
    { ...good, ranked: [42] }, { ...good, rankingVersion: 'future' }, { ...good, matchingVersion: 'future' },
    { ...good, apiVersion: 'future' }]) expect(() => parse(bad)).toThrow();
});
