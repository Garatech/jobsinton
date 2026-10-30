import assert from 'node:assert/strict';
import fs from 'node:fs/promises';
import path from 'node:path';
import { createRequire } from 'node:module';
import { pathToFileURL } from 'node:url';
const require = createRequire(path.resolve('frontend/package.json'));
const { chromium, expect } = require('@playwright/test');
const root = path.resolve('design-preversion');
const out = path.resolve('current-preversion-evidence');
await fs.mkdir(out, { recursive: true });
const pages = (await fs.readdir(root)).filter(p => p.endsWith('.html')).sort();
assert.equal(pages.length, 14, 'The complete preview contains 14 standalone pages');
const sources = new Map(await Promise.all(pages.map(async p => [p, await fs.readFile(path.join(root, p), 'utf8')])));
for (const [file, html] of sources) {
  assert.equal((html.match(/<h1[ >]/g) || []).length, 1, file + ' needs one main heading');
  for (const match of html.matchAll(/(?:href|src)="([^"]+)"/g)) {
    const raw = match[1].replaceAll('&amp;', '&');
    assert.ok(!/^https?:/.test(raw), 'The preview must be self-contained');
    const [relative, fragment] = raw.split('#');
    const target = relative.split('?')[0] || file;
    await fs.access(path.join(root, target));
    if (fragment && sources.has(target)) assert.ok(sources.get(target).includes('id="' + fragment + '"'), file + ': missing anchor ' + raw);
  }
}
const browser = await chromium.launch();
const results = [];
const errors = [];
let active;
const url = file => pathToFileURL(path.join(root, file)).href;
async function fit(page, label) {
  const dimensions = await page.evaluate(() => ({ viewport: innerWidth, document: document.documentElement.scrollWidth }));
  if (dimensions.document > dimensions.viewport + 1) {
    console.log('Overflow', label, await page.evaluate(() => [...document.querySelectorAll('body *')].filter(el => { const r = el.getBoundingClientRect(); return r.width && r.right > innerWidth + 1; }).slice(0, 12).map(el => ({ tag: el.tagName, cls: el.className, right: el.getBoundingClientRect().right }))));
  }
  assert.ok(dimensions.document <= dimensions.viewport + 1, label + ': horizontal overflow ' + JSON.stringify(dimensions));
}
async function contrast(locator) {
  const colors = await locator.evaluate(el => { const s = getComputedStyle(el); return [s.color, s.backgroundColor]; });
  const luminance = value => {
    const [r, g, b] = value.match(/[\d.]+/g).slice(0, 3).map(Number).map(v => v / 255).map(v => v <= .04045 ? v / 12.92 : ((v + .055) / 1.055) ** 2.4);
    return r * .2126 + g * .7152 + b * .0722;
  };
  const a = luminance(colors[0]), b = luminance(colors[1]);
  assert.ok((Math.max(a, b) + .05) / (Math.min(a, b) + .05) >= 4.5, 'Action text contrast: ' + colors.join(' / '));
}
try {
  for (const width of [320, 390, 768, 1440]) {
    console.log('Checking 14 pages at', width);
    const context = await browser.newContext({ viewport: { width, height: 960 } });
    const page = await context.newPage(); active = page;
    page.on('pageerror', error => errors.push(error.message));
    page.on('request', request => { if (/^https?:/.test(request.url())) errors.push('Unexpected network: ' + request.url()); });
    for (const file of pages) {
      await page.goto(url(file));
      await expect(page.locator('h1')).toBeVisible();
      await expect(page.locator('body')).toHaveCSS('background-color', 'rgb(245, 245, 239)');
      await expect(page.locator('h1')).toHaveCSS('font-family', /Segoe UI/);
      await fit(page, file + ' ' + width);
      await page.screenshot({ path: path.join(out, file.replace('.html', '') + '-' + width + '.png'), fullPage: true });
      results.push({ page: file, width, result: 'passed' });
    }
    await page.goto(url('index.html'));
    await page.keyboard.press('Tab');
    await expect(page.locator('.skip-link')).toBeFocused();
    await page.keyboard.press('Enter');
    await expect(page.locator('#main')).toBeFocused();
    await contrast(page.locator('.hero .primary'));
    await page.locator('.hero .primary').hover();
    await contrast(page.locator('.hero .primary'));
    await page.locator('.hero .primary').click();
    await expect(page.locator('.job:visible')).toHaveCount(6);
    await page.locator('#query').fill('java');
    await expect(page.locator('.job:visible')).toHaveCount(1);
    await page.locator('#role-filter').selectOption('SAP');
    await expect(page.locator('#empty-state')).toBeVisible();
    await page.locator('#empty-reset').click();
    await expect(page.locator('.job:visible')).toHaveCount(6);
    await page.locator('input[name="mode"][value="Remote"]').check();
    await expect(page.locator('.job:visible')).toHaveCount(3);
    await page.locator('#salary-filter').selectOption('45000');
    await expect(page.locator('.job:visible')).toHaveCount(2);
    await page.locator('#role-filter').selectOption('Backend');
    await expect(page.locator('.job:visible')).toHaveCount(1);
    await page.getByRole('button', { name: 'Remove filter: Backend', exact: true }).click();
    await expect(page.locator('.job:visible')).toHaveCount(2);
    await page.locator('#clear-filters').click();
    await page.locator('#sort').selectOption('salary');
    await expect(page.locator('.job').first()).toHaveAttribute('data-job', 'platform-engineer');
    await page.getByRole('button', { name: 'Card view', exact: true }).click();
    await expect(page.locator('.jobs')).toHaveClass(/cards/);
    await fit(page, 'Card search ' + width);
    await page.screenshot({ path: path.join(out, 'search-cards-' + width + '.png'), fullPage: true });
    await page.getByRole('button', { name: 'List view', exact: true }).click();
    await page.locator('[data-save="frontend-react"]').focus();
    await page.keyboard.press('Enter');
    await expect(page.locator('.saved-count')).toHaveText('3');
    await page.locator('.site-nav a[href^="saved.html"]').click();
    await expect(page.locator('.job:visible')).toHaveCount(3);
    for (let count = 3; count > 0; count--) {
      await page.locator('.job:visible [data-save]').first().click();
      await expect(page.locator('.job:visible')).toHaveCount(count - 1);
    }
    await expect(page.locator('#empty-state')).toBeVisible();
    await expect(page.locator('#empty-state a')).toBeFocused();
    await page.locator('#empty-state a').click();
    await page.locator('[data-save="backend-java"]').click();
    await page.locator('h3 a[href^="job-backend-java.html"]').focus();
    await page.keyboard.press('Tab');
    await page.keyboard.press('Shift+Tab');
    await expect(page.locator('h3 a[href^="job-backend-java.html"]')).toBeFocused();
    assert.notEqual(await page.locator('h3 a[href^="job-backend-java.html"]').evaluate(el => getComputedStyle(el).outlineStyle), 'none');
    await page.keyboard.press('Enter');
    await expect(page.locator('.apply-card [data-save]')).toHaveAttribute('aria-pressed', 'true');
    await page.locator('#apply-open').click();
    await expect(page.locator('#apply-dialog')).toHaveJSProperty('open', true);
    await fit(page, 'Application dialog ' + width);
    await page.keyboard.press('Escape');
    await expect(page.locator('#apply-open')).toBeFocused();
    await page.locator('details summary').click();
    await expect(page.locator('details p')).toBeVisible();

    await page.locator('.site-nav a[href^="profile.html"]').click();
    await page.locator('#profile-city').fill('');
    await page.getByRole('button', { name: 'Save preferences', exact: true }).click();
    await expect(page.locator('#profile-error')).toBeVisible();
    await expect(page.locator('#profile-city')).toBeFocused();
    await page.locator('#profile-city').fill('Valencia');
    await expect(page.locator('#summary-location')).toHaveText('Madrid, Spain');
    await page.locator('#profile-mode').selectOption('Hybrid');
    await expect(page.locator('#days-field')).toBeVisible();
    await page.getByRole('button', { name: 'Save preferences', exact: true }).click();
    await expect(page.locator('#profile-days')).toBeFocused();
    await page.locator('#profile-days').selectOption('2');
    await page.locator('#technology').fill('java');
    await page.locator('#add-technology').click();
    await expect(page.locator('#technology-error')).toHaveText(/already added/);
    await page.locator('#technology').fill('<sample>');
    await page.locator('#add-technology').click();
    await page.locator('#profile-years').fill('-1');
    await page.getByRole('button', { name: 'Save preferences', exact: true }).click();
    await expect(page.locator('#profile-years')).toBeFocused();
    await page.locator('#profile-years').fill('0');
    await page.getByRole('button', { name: 'Save preferences', exact: true }).click();
    await expect(page.locator('#summary-location')).toHaveText('Valencia, Spain');
    await expect(page.locator('#summary-tech')).toContainText('<sample>');
    await expect(page.locator('#summary-years')).toHaveText('0 years');
    await expect(page.locator('#profile-message')).toContainText('saved');
    await page.locator('#profile-city').fill('C'.repeat(80));
    await page.locator('#technology').fill('T'.repeat(40));
    await page.locator('#add-technology').click();
    await page.getByRole('button', { name: 'Save preferences', exact: true }).click();
    await fit(page, 'Long profile values ' + width);
    await page.locator('#use-example').click();
    await page.locator('#profile-role').selectOption('Data engineer');
    await page.locator('#profile-mode').selectOption('Hybrid');
    await page.locator('#profile-days').selectOption('2');
    await page.getByRole('button', { name: 'Save preferences', exact: true }).click();
    await page.locator('#profile-explore').click();
    await expect(page.locator('.job:visible')).toHaveCount(1);
    await expect(page.locator('.job:visible')).toHaveAttribute('data-job', 'data-engineer');
    await page.locator('.site-nav a[href^="profile.html"]').click();
    await expect(page.locator('#profile-role')).toHaveValue('Data engineer');
    await page.locator('#profile-city').fill('Seville');
    await page.locator('#discard-changes').click();
    await expect(page.locator('#profile-city')).toHaveValue('Madrid');

    await page.locator('.site-nav a[href^="companies.html"]').click();
    await page.locator('#company-query').fill('not-a-company');
    await expect(page.locator('#empty-state')).toBeVisible();
    await page.locator('#clear-company-search').click();
    await expect(page.locator('.company-card:visible')).toHaveCount(3);
    await page.locator('#company-query').fill('Valencia');
    await expect(page.locator('.company-card:visible')).toHaveCount(1);
    await page.locator('.company-card:visible h2 a').click();
    await expect(page.locator('#open-roles .job')).toHaveCount(2);

    await page.locator('#reset-demo').click();
    await expect(page.locator('#reset-dialog')).toHaveJSProperty('open', true);
    await page.keyboard.press('Escape');
    await expect(page.locator('#reset-demo')).toBeFocused();
    await page.locator('#reset-demo').click();
    await page.locator('#confirm-reset').click();
    await expect(page.locator('.saved-count')).toHaveText('2');
    await page.locator('.site-nav a[href^="profile.html"]').click();
    await expect(page.locator('#profile-role')).toHaveValue('Backend developer');
    assert.equal(errors.length, 0, errors.join('\n'));
    console.log('Passed journey at', width);
    await context.close();
  }
  // The saved selection must still travel between file pages if browser storage is blocked.
  const blocked = await browser.newContext({ viewport: { width: 390, height: 960 } });
  await blocked.addInitScript(() => {
    Object.defineProperty(window, 'sessionStorage', { get() { throw new DOMException('Blocked', 'SecurityError'); } });
  });
  const page = await blocked.newPage(); active = page;
  await page.goto(url('opportunities.html') + '?saved=');
  await page.locator('[data-save="frontend-react"]').click();
  await page.locator('.site-nav a[href^="saved.html"]').click();
  await expect(page.locator('.job:visible')).toHaveCount(1);
  await expect(page.locator('.job:visible')).toHaveAttribute('data-job', 'frontend-react');
  await page.locator('.site-nav a[href^="profile.html"]').click();
  await page.getByRole('button', { name: 'Save preferences', exact: true }).click();
  await expect(page.locator('#profile-message')).toContainText('storage is unavailable');
  await blocked.close();
  const result = { status: 'passed', sourceCommit: process.env.GITHUB_SHA || 'local', browser: 'Chromium', transport: 'file://', pageViewportCases: results, interactiveJourneys: 4, blockedStorage: 'passed', networkRequests: 0 };
  await fs.writeFile(path.join(root, 'VERIFICATION_RESULT.json'), JSON.stringify(result, null, 2));
  await fs.writeFile(path.join(out, 'verification.json'), JSON.stringify(result, null, 2));
  console.log(JSON.stringify({ status: 'passed', pageViewportCases: results.length, interactiveJourneys: 4, blockedStorage: 'passed' }));
} catch (error) {
  if (active && !active.isClosed()) await active.screenshot({ path: path.join(out, 'failure.png'), fullPage: true }).catch(() => {});
  await fs.writeFile(path.join(out, 'failure.txt'), String(error.stack || error));
  throw error;
} finally { await browser.close(); }
