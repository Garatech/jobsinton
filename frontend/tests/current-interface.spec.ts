import { expect, test } from '@playwright/test';
import { opportunities, companies } from '../src/catalog/data';
import { PROFILE_STORAGE_KEY } from '../src/profile/storage';
import { SAVED_STORAGE_KEY } from '../src/catalog/saved';

const profile = { role: 'backend', technologies: ['Java', 'Spring Boot'], experienceYears: 3, country: 'Spain', city: 'Madrid', workMode: 'remote', maxOnsiteDays: null };

test('searches, combines filters, orders results and retains the selected layout on reload', async ({ page }) => {
  await page.goto('/#opportunities');
  await expect(page.locator('.job')).toHaveCount(6);
  await page.getByLabel('Search opportunities').fill('Northline Studio');
  await expect(page.locator('.job')).toHaveCount(2);
  await page.getByLabel('Search opportunities').fill('java');
  await expect(page.locator('.job')).toHaveCount(1);
  await page.getByLabel('Role', { exact: true }).selectOption('SAP');
  await expect(page.getByText('No opportunities found.')).toBeVisible();
  await page.getByRole('button', { name: 'Clear filters', exact: true }).click();
  await page.getByLabel('Remote', { exact: true }).check();
  await expect(page.locator('.job')).toHaveCount(3);
  await page.getByLabel('Minimum salary').selectOption('45000');
  await expect(page.locator('.job')).toHaveCount(2);
  await page.getByLabel('Role', { exact: true }).selectOption('Backend');
  await expect(page.locator('.job')).toHaveCount(1);
  await page.getByRole('button', { name: 'Remove filter: Backend', exact: true }).click();
  await expect(page.locator('.job')).toHaveCount(2);
  await page.getByLabel('Sort by').selectOption('salary');
  await expect(page.locator('.job').first()).toHaveAttribute('data-job', 'platform-engineer');
  await page.getByRole('button', { name: 'Card view', exact: true }).click();
  await expect(page.locator('.jobs')).toHaveClass(/cards/);
  await page.reload();
  await expect(page.getByRole('button', { name: 'Card view', exact: true })).toHaveAttribute('aria-pressed', 'true');
  await expect(page.locator('.job')).toHaveCount(2);
  await page.getByRole('button', { name: 'Reset', exact: true }).click();
  await expect(page.locator('.job')).toHaveCount(6);
  await page.getByLabel('Search opportunities').fill('java');
  await page.locator('.site-nav a[href="#opportunities"]').click();
  await expect(page.getByLabel('Search opportunities')).toHaveValue('');
  await expect(page.locator('.job')).toHaveCount(6);
});

test('saves across navigation and reload, then restores keyboard focus in the empty shortlist', async ({ page }) => {
  await page.goto('/#opportunities');
  expect(await page.evaluate(key => localStorage.getItem(key), SAVED_STORAGE_KEY)).toBeNull();
  await page.getByRole('button', { name: 'Save Backend Developer', exact: true }).focus();
  await page.keyboard.press('Enter');
  await expect(page.locator('.saved-count')).toHaveText('1');
  await page.getByRole('link', { name: 'Backend Developer', exact: true }).click();
  await expect(page.locator('.apply-card [data-save]')).toHaveAttribute('aria-pressed', 'true');
  await page.getByRole('button', { name: 'Apply for this role' }).click();
  await expect(page.getByRole('dialog')).toBeVisible();
  await expect(page.getByRole('dialog')).toContainText('No application will be sent');
  await page.keyboard.press('Escape');
  await expect(page.getByRole('button', { name: 'Apply for this role' })).toBeFocused();
  await page.locator('.site-nav a[href="#saved"]').click();
  await expect(page.locator('.job')).toHaveCount(1);
  await page.reload();
  await expect(page.locator('.job')).toHaveCount(1);
  await page.getByRole('button', { name: 'Remove Backend Developer from saved', exact: true }).click();
  await expect(page.locator('.job')).toHaveCount(0);
  await expect(page.locator('.empty-state a')).toBeFocused();
  await page.reload();
  await expect(page.locator('.saved-count')).toHaveText('0');
});

test('companies, direct links, browser history and unknown routes work', async ({ page }) => {
  await page.goto('/#companies');
  await page.getByLabel('Search companies').fill('Valencia');
  await expect(page.locator('.company-card')).toHaveCount(1);
  await page.getByRole('link', { name: 'Vela Systems', exact: true }).click();
  await expect(page.getByRole('heading', { level: 1 })).toHaveText('Vela Systems.');
  await page.getByRole('link', { name: 'Explore open roles' }).click();
  await expect(page.locator('#open-roles')).toBeFocused();
  await page.getByRole('link', { name: 'Data Engineer', exact: true }).click();
  await page.reload();
  await expect(page.getByRole('heading', { level: 1 })).toHaveText('Data Engineer.');
  await page.goBack();
  await expect(page.getByRole('heading', { level: 1 })).toHaveText('Vela Systems.');
  await page.goto('/#opportunities/missing');
  await expect(page.getByRole('heading', { name: 'Page not found.' })).toBeVisible();
  await page.goto('/#companies/northline/extra');
  await expect(page.getByRole('heading', { name: 'Page not found.' })).toBeVisible();
  await page.goto('/#missing');
  await expect(page.getByRole('heading', { name: 'Page not found.' })).toBeVisible();
  await page.getByRole('link', { name: 'Explore opportunities', exact: true }).click();
  await expect(page.locator('.job')).toHaveCount(6);
});

test('existing v1 profiles remain intact and drive the shared interface only after saving', async ({ page }) => {
  await page.goto('/');
  const raw = JSON.stringify({ version: 1, profile });
  await page.evaluate(({ key, raw }) => localStorage.setItem(key, raw), { key: PROFILE_STORAGE_KEY, raw });
  await page.reload();
  await expect(page.locator('.hero .connection-card .card-description')).toContainText('Backend developer');
  expect(await page.evaluate(key => localStorage.getItem(key), PROFILE_STORAGE_KEY)).toBe(raw);
  await page.getByRole('link', { name: 'Explore this direction' }).click();
  await expect(page.locator('.job')).toHaveCount(1);
  await page.getByRole('link', { name: 'Backend Developer', exact: true }).click();
  await expect(page.locator('.fit-section')).toContainText('Java, Spring Boot');
  await page.locator('.site-nav a[href="#profile"]').click();
  await page.getByLabel('City of residence').fill('Valencia');
  await page.locator('.site-nav a[href="#home"]').click();
  await page.locator('.site-nav a[href="#profile"]').click();
  await expect(page.getByLabel('City of residence')).toHaveValue('Valencia');
  await expect(page.getByRole('complementary', { name: 'Saved profile' })).toContainText('Madrid');
  await page.getByRole('button', { name: 'Discard changes', exact: true }).click();
  await expect(page.getByLabel('City of residence')).toHaveValue('Madrid');
  await page.getByLabel('Target role', { exact: true }).selectOption('data');
  await page.getByLabel('Preferred work mode').selectOption('hybrid');
  await page.getByLabel('Maximum on-site days per week').selectOption('2');
  await page.getByRole('button', { name: 'Save profile', exact: true }).click();
  await page.getByRole('complementary', { name: 'Saved profile' }).getByRole('link', { name: 'Explore opportunities' }).click();
  await expect(page.locator('.job')).toHaveCount(1);
  await expect(page.locator('.job')).toHaveAttribute('data-job', 'data-engineer');
});

test('failed shortlist writes and clears keep the persisted data and profile', async ({ page }) => {
  await page.goto('/#opportunities');
  await page.getByRole('button', { name: 'Save Backend Developer', exact: true }).click();
  await page.evaluate(({ key, profile }) => {
    localStorage.setItem(key, JSON.stringify({ version: 1, profile }));
    Storage.prototype.setItem = () => { throw new DOMException('Full', 'QuotaExceededError'); };
    Storage.prototype.removeItem = () => { throw new DOMException('Blocked', 'SecurityError'); };
  }, { key: PROFILE_STORAGE_KEY, profile });
  await page.getByRole('button', { name: 'Save Frontend Developer', exact: true }).click();
  await expect(page.getByText(/The shortlist could not be saved/)).toBeVisible();
  await expect(page.locator('.saved-count')).toHaveText('1');
  await expect(page.getByRole('button', { name: 'Save Frontend Developer', exact: true })).toHaveAttribute('aria-pressed', 'false');
  await page.locator('.site-nav a[href="#saved"]').click();
  page.once('dialog', dialog => dialog.accept());
  await page.getByRole('button', { name: 'Clear shortlist', exact: true }).click();
  await expect(page.getByText(/The shortlist could not be cleared/)).toBeVisible();
  await expect(page.locator('.job')).toHaveCount(1);
  await page.reload();
  await expect(page.locator('.job')).toHaveCount(1);
  expect(await page.evaluate(key => JSON.parse(localStorage.getItem(key)!).profile.city, PROFILE_STORAGE_KEY)).toBe('Madrid');
});

for (const raw of ['{broken', '{"version":2,"ids":[]}', '{"version":1,"ids":[null]}']) {
  test('recovers malformed shortlist without writing on mount: ' + raw, async ({ page }) => {
    await page.goto('/#saved');
    await page.evaluate(({ key, raw }) => localStorage.setItem(key, raw), { key: SAVED_STORAGE_KEY, raw });
    await page.reload();
    await expect(page.getByText(/Saved opportunities could not be read/)).toBeVisible();
    expect(await page.evaluate(key => localStorage.getItem(key), SAVED_STORAGE_KEY)).toBe(raw);
    page.once('dialog', dialog => dialog.dismiss());
    await page.getByRole('button', { name: 'Clear shortlist', exact: true }).click();
    expect(await page.evaluate(key => localStorage.getItem(key), SAVED_STORAGE_KEY)).toBe(raw);
    page.once('dialog', dialog => dialog.accept());
    await page.getByRole('button', { name: 'Clear shortlist', exact: true }).click();
    expect(await page.evaluate(key => localStorage.getItem(key), SAVED_STORAGE_KEY)).toBeNull();
    await expect(page.getByText(/Saved opportunities could not be read/)).toHaveCount(0);
  });
}

test('blocked storage allows browsing without pretending to save', async ({ page }) => {
  await page.addInitScript(() => Object.defineProperty(window, 'localStorage', { get() { throw new DOMException('Blocked', 'SecurityError'); } }));
  await page.goto('/#opportunities');
  await expect(page.locator('.job')).toHaveCount(6);
  await page.getByRole('button', { name: 'Save Backend Developer', exact: true }).click();
  await expect(page.locator('.saved-count')).toHaveText('0');
  await expect(page.getByText(/The shortlist could not be saved/)).toBeVisible();
});

test('saved records discard unknown IDs safely and update other open tabs', async ({ page, context }) => {
  await page.goto('/#saved');
  await page.evaluate(key => localStorage.setItem(key, JSON.stringify({ version: 1, ids: ['backend-java', 'missing', 'backend-java'] })), SAVED_STORAGE_KEY);
  await page.reload();
  await expect(page.locator('.job')).toHaveCount(1);
  const second = await context.newPage();
  await second.goto('/#opportunities');
  await second.getByRole('button', { name: 'Save Frontend Developer', exact: true }).click();
  await expect(page.locator('.job')).toHaveCount(2);
  await second.close();
});

for (const width of [320, 390, 768, 1440]) {
  test('Current page layout and screenshots at ' + width, async ({ page }, testInfo) => {
    test.skip(testInfo.project.name !== 'desktop-chromium', 'One run per explicit viewport is sufficient.');
    await page.setViewportSize({ width, height: 960 });
    const errors: string[] = [];
    page.on('pageerror', error => errors.push(error.message));
    const routes = ['home', 'opportunities', 'companies', 'saved', 'profile', 'status', ...opportunities.map(j => 'opportunities/' + j.id), ...companies.map(c => 'companies/' + c.id)];
    await page.goto('/');
    await page.evaluate(({ key, profile }) => localStorage.setItem(key, JSON.stringify({ version: 1, profile })), { key: PROFILE_STORAGE_KEY, profile });
    for (const route of routes) {
      await page.goto('/#' + route);
      await expect(page.getByRole('heading', { level: 1 })).toBeVisible();
      await expect(page.locator('body')).toHaveCSS('background-color', 'rgb(245, 245, 239)');
      await expect(page.getByRole('heading', { level: 1 })).toHaveCSS('font-family', /Segoe UI/);
      expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth), route).toBe(true);
      await page.screenshot({ path: testInfo.outputPath('current-' + route.replace('/', '-') + '-' + width + '.png'), fullPage: true });
    }
    await page.goto('/#opportunities?view=cards');
    await expect(page.locator('.jobs')).toHaveClass(/cards/);
    expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBe(true);
    await page.screenshot({ path: testInfo.outputPath('current-cards-' + width + '.png'), fullPage: true });
    expect(errors).toEqual([]);
  });
}
