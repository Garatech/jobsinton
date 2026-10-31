import { expect, test, type Page } from '@playwright/test';
import { opportunities } from '../src/catalog/data';
import { parseComparison } from '../src/api/matching';
import { PROFILE_STORAGE_KEY } from '../src/profile/storage';

const endpoint = '/api/matching/evaluate';
const profile = { role: 'backend', technologies: ['Java', 'Spring Boot'], experienceYears: 3,
  country: 'Spain', city: 'Madrid', workMode: 'remote', maxOnsiteDays: null };

async function seed(page: Page, value: object = profile) {
  await page.addInitScript(({ key, value }) => localStorage.setItem(key, JSON.stringify({ version: 1, profile: value })),
    { key: PROFILE_STORAGE_KEY, value });
}
const panel = (page: Page) => page.getByRole('region', { name: 'How it lines up' });

test('real API compares only the saved profile and retains unknowns in the Current panel', async ({ page }, info) => {
  await seed(page);
  const request = page.waitForRequest(r => r.url().endsWith(endpoint));
  await page.goto('/#opportunities/backend-java');
  expect((await request).postDataJSON()).toEqual({ opportunityId: 'backend-java', profile });
  await expect(panel(page).getByRole('status')).toHaveText('Some conditions still need confirmation.');
  await expect(panel(page)).toContainText('Technologies in common: Java, Spring Boot.');
  await panel(page).getByText('What’s still unknown or not assessed', { exact: true }).click();
  await expect(panel(page).locator('[data-criterion="experience"]')).toContainText('Total profile years cannot confirm');
  await expect(panel(page).locator('[data-criterion="residence"]')).toHaveAttribute('data-outcome', 'UNKNOWN');
  await expect(panel(page).locator('[data-criterion="salary"]')).toHaveAttribute('data-outcome', 'NOT_ASSESSED');
  await expect(panel(page).locator('[data-criterion="technology:postgresql"]')).toContainText('not confirmed');
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBe(true);
  await page.screenshot({ path: info.outputPath('current-matching.png'), fullPage: true });
  await page.reload();
  await expect(panel(page)).toContainText('Technologies in common: Java, Spring Boot.');
});

test('a profile is required and browsing or saving alone sends no comparison', async ({ page }) => {
  const requests: string[] = [];
  page.on('request', r => { if (r.url().endsWith(endpoint)) requests.push(r.url()); });
  await page.goto('/#opportunities/backend-java');
  await expect(panel(page).getByRole('link', { name: 'Set up your profile' })).toBeVisible();
  await page.getByRole('button', { name: 'Save Backend Developer', exact: true }).click();
  await expect(page.locator('.saved-count')).toHaveText('1');
  expect(requests).toHaveLength(0);
});

test('office-day conflicts keep the unresolved residence explanation', async ({ page }) => {
  await seed(page, { ...profile, city: 'València', country: 'España', workMode: 'hybrid', maxOnsiteDays: 1 });
  await page.goto('/#opportunities/data-engineer');
  await expect(panel(page).getByRole('status')).toHaveText('A stated limit is not met.');
  await expect(panel(page).locator('[data-criterion="attendance"]')).toContainText('exceed');
  await expect(panel(page).locator('[data-criterion="office-location"]')).toHaveAttribute('data-outcome', 'ALIGNED');
  await panel(page).locator('summary').click();
  await expect(panel(page).locator('[data-criterion="residence"]')).toHaveAttribute('data-outcome', 'UNKNOWN');
});

test('server failure allows shortlist actions and a keyboard retry', async ({ page }) => {
  await seed(page);
  let fail = true;
  await page.route('**' + endpoint, async route => {
    if (fail) await route.fulfill({ status: 503, contentType: 'application/json', body: '{}' });
    else await route.continue();
  });
  await page.goto('/#opportunities/backend-java');
  await expect(panel(page).getByRole('alert')).toContainText('unavailable');
  await expect(panel(page).locator('[data-comparison-for]')).toHaveCount(0);
  await page.getByRole('button', { name: 'Save Backend Developer', exact: true }).click();
  await expect(page.locator('.saved-count')).toHaveText('1');
  fail = false;
  await panel(page).getByRole('button', { name: 'Retry comparison' }).focus();
  await page.keyboard.press('Enter');
  await expect(panel(page)).toContainText('Technologies in common: Java, Spring Boot.');
});

test('timeout produces recovery feedback without a late success', async ({ page, request }) => {
  await seed(page);
  const real = await (await request.post(endpoint, { data: { opportunityId: 'backend-java', profile } })).json();
  await page.clock.install();
  let release!: () => void;
  let captured!: () => void;
  const pending = new Promise<void>(resolve => { release = resolve; });
  const started = new Promise<void>(resolve => { captured = resolve; });
  await page.route('**' + endpoint, async route => {
    captured(); await pending;
    await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify(real) }).catch(() => {});
  });
  await page.goto('/#opportunities/backend-java');
  await started;
  await expect(panel(page).getByRole('status')).toContainText('Comparing');
  await page.clock.fastForward(8001);
  await expect(panel(page).getByRole('alert')).toContainText('took too long');
  release();
  await expect(panel(page).locator('[data-comparison-for]')).toHaveCount(0);
  await page.unrouteAll({ behavior: 'wait' });
  await panel(page).getByRole('button', { name: 'Retry comparison' }).click();
  await expect(panel(page)).toContainText('Technologies in common: Java, Spring Boot.');
});

test('wrong-opportunity response cannot be displayed as a comparison', async ({ page, request }) => {
  await seed(page);
  const wrong = await (await request.post(endpoint, { data: { opportunityId: 'sap-developer', profile } })).json();
  await page.route('**' + endpoint, route => route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify(wrong) }));
  await page.goto('/#opportunities/backend-java');
  await expect(panel(page).getByRole('alert')).toContainText('could not be verified');
  await expect(panel(page).locator('[data-comparison-for]')).toHaveCount(0);
});

test('an old offer response cannot replace the newly opened offer even if cancellation is ignored', async ({ page }) => {
  await seed(page);
  await page.addInitScript(() => {
    const original = window.fetch.bind(window);
    window.fetch = (input, init) => original(input, String(input).endsWith('/api/matching/evaluate')
      ? { ...init, signal: undefined } : init);
  });
  let release!: () => void;
  let captured!: () => void;
  const pending = new Promise<void>(resolve => { release = resolve; });
  const started = new Promise<void>(resolve => { captured = resolve; });
  await page.route('**' + endpoint, async route => {
    if (route.request().postDataJSON().opportunityId !== 'backend-java') return route.continue();
    const response = await route.fetch();
    captured(); await pending;
    await route.fulfill({ response });
  });
  await page.goto('/#opportunities/backend-java');
  await started;
  await page.evaluate(() => { location.hash = '#opportunities/sap-developer'; });
  await expect(panel(page).locator('[data-comparison-for="sap-developer"]')).toBeVisible();
  const oldResponse = page.waitForResponse(r => r.url().endsWith(endpoint) && r.request().postDataJSON().opportunityId === 'backend-java');
  release();
  await (await oldResponse).finished();
  await page.evaluate(() => new Promise<void>(resolve => requestAnimationFrame(() => requestAnimationFrame(() => resolve()))));
  await expect(panel(page).locator('[data-comparison-for]')).toHaveAttribute('data-comparison-for', 'sap-developer');
  await expect(panel(page)).not.toContainText('Technologies in common: Java');
  await page.goBack();
  await expect(panel(page).locator('[data-comparison-for="backend-java"]')).toBeVisible();
});

test('drafts and failed saves do not change comparisons; successful saves and clear do', async ({ page }) => {
  await seed(page);
  const sent: typeof profile[] = [];
  page.on('request', r => { if (r.url().endsWith(endpoint)) sent.push(r.postDataJSON().profile); });
  await page.goto('/#opportunities/data-engineer');
  await expect(panel(page).locator('[data-comparison-for]')).toBeVisible();
  await page.locator('.site-nav a[href="#profile"]').click();
  await page.getByLabel('Preferred work mode').selectOption('hybrid');
  await page.getByLabel('Maximum on-site days per week').selectOption('1');
  await page.evaluate(() => { location.hash = '#opportunities/data-engineer'; });
  await expect(panel(page).getByRole('status')).toHaveText('Some conditions still need confirmation.');
  expect(sent.at(-1)?.workMode).toBe('remote');
  await page.locator('.site-nav a[href="#profile"]').click();
  await page.evaluate(() => {
    const original = Storage.prototype.setItem;
    Storage.prototype.setItem = function (key, value) {
      if (key === 'jobsinton.professional-profile') throw new DOMException('Full', 'QuotaExceededError');
      return original.call(this, key, value);
    };
  });
  await page.getByRole('button', { name: 'Save profile', exact: true }).click();
  await expect(page.getByRole('alert')).toContainText('could not be saved');
  await page.evaluate(() => { location.hash = '#opportunities/data-engineer'; });
  await expect(panel(page).getByRole('status')).toHaveText('Some conditions still need confirmation.');
  expect(sent.at(-1)?.workMode).toBe('remote');
  // A reload restores the native storage method; the persisted remote snapshot remains intact.
  await page.reload();
  await page.locator('.site-nav a[href="#profile"]').click();
  await page.getByLabel('Preferred work mode').selectOption('hybrid');
  await page.getByLabel('Maximum on-site days per week').selectOption('1');
  await page.getByRole('button', { name: 'Save profile', exact: true }).click();
  await page.evaluate(() => { location.hash = '#opportunities/data-engineer'; });
  await expect(panel(page).getByRole('status')).toHaveText('A stated limit is not met.');
  expect(sent.at(-1)?.maxOnsiteDays).toBe(1);
  await page.locator('.site-nav a[href="#profile"]').click();
  page.once('dialog', dialog => dialog.accept());
  await page.getByRole('button', { name: 'Clear saved profile', exact: true }).click();
  const count = sent.length;
  await page.evaluate(() => { location.hash = '#opportunities/data-engineer'; });
  await expect(panel(page).getByRole('link', { name: 'Set up your profile' })).toBeVisible();
  expect(sent).toHaveLength(count);
});

test('catalog comparison facts stay aligned with the displayed fictional listings', async ({ request }) => {
  for (const job of opportunities) {
    const response = await request.post(endpoint, { data: { opportunityId: job.id, profile } });
    expect(response.status()).toBe(200);
    const result = parseComparison(await response.json(), job.id);
    const assessments = result.evaluation.assessments;
    const fact = (id: string) => assessments.find(a => a.criterion === id)!;
    expect(fact('role').evidence.opportunityValues).toEqual([job.role.toUpperCase().replace(' ', '_')]);
    expect(fact('work-mode').evidence.opportunityValues).toEqual([job.mode.toUpperCase().replace('-', '')]);
    expect(fact('attendance').evidence.opportunityValues).toEqual([String(job.officeDays)]);
    expect(assessments.filter(a => a.dimension === 'TECHNOLOGY').flatMap(a => a.evidence.opportunityValues).sort()).toEqual([...job.tags].sort());
    expect(fact('office-location').evidence.opportunityValues).toEqual(['country=ES', 'city=' + job.city.toLowerCase()]);
    expect(fact('experience').evidence.opportunityValues).toContain('minimumYears=' + job.minimumYears);
    expect(fact('residence').reason).toBe('RESIDENCE_POLICY_UNKNOWN');
  }
});

test('response validation rejects incompatible versions, missing criteria and invalid evidence', async ({ request }) => {
  const response = await request.post(endpoint, { data: { opportunityId: 'backend-java', profile } });
  const good = await response.json();
  expect(() => parseComparison(good, 'backend-java')).not.toThrow();
  const changes = [
    (v: typeof good) => { v.apiVersion = 'future'; },
    (v: typeof good) => { v.evaluation.rulesVersion = 'future'; },
    (v: typeof good) => { v.evaluation.assessments = []; },
    (v: typeof good) => { v.evaluation.assessments[0].outcome = 'PERFECT'; },
    (v: typeof good) => { v.evaluation.assessments[0].evidence.profileValues = [null]; },
    (v: typeof good) => { v.evaluation.assessments.push(v.evaluation.assessments[0]); },
  ];
  for (const change of changes) {
    const bad = structuredClone(good); change(bad);
    expect(() => parseComparison(bad, 'backend-java')).toThrow();
  }
});

test('a superseded profile response cannot overwrite a newly saved comparison', async ({ page }) => {
  await seed(page);
  await page.addInitScript(() => {
    const original = window.fetch.bind(window);
    window.fetch = (input, init) => original(input, String(input).endsWith('/api/matching/evaluate')
      ? { ...init, signal: undefined } : init);
  });
  let release!: () => void;
  let captured!: () => void;
  const pending = new Promise<void>(resolve => { release = resolve; });
  const started = new Promise<void>(resolve => { captured = resolve; });
  await page.route('**' + endpoint, async route => {
    if (route.request().postDataJSON().profile.workMode !== 'remote') return route.continue();
    const response = await route.fetch(); captured(); await pending;
    await route.fulfill({ response });
  });
  await page.goto('/#opportunities/data-engineer');
  await started;
  await page.locator('.site-nav a[href="#profile"]').click();
  await page.getByLabel('Preferred work mode').selectOption('hybrid');
  await page.getByLabel('Maximum on-site days per week').selectOption('1');
  await page.getByRole('button', { name: 'Save profile', exact: true }).click();
  await page.evaluate(() => { location.hash = '#opportunities/data-engineer'; });
  await expect(panel(page).getByRole('status')).toHaveText('A stated limit is not met.');
  const oldResponse = page.waitForResponse(r => r.url().endsWith(endpoint) && r.request().postDataJSON().profile.workMode === 'remote');
  release();
  await (await oldResponse).finished();
  await page.evaluate(() => new Promise<void>(resolve => requestAnimationFrame(() => requestAnimationFrame(() => resolve()))));
  await expect(panel(page).getByRole('status')).toHaveText('A stated limit is not met.');
});
