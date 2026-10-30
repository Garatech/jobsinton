import { expect, test, type Page } from '@playwright/test';
import { PROFILE_STORAGE_KEY } from '../src/profile/storage';

async function fillProfile(page: Page, mode = 'remote') {
  await page.getByLabel('Target role', { exact: true }).selectOption('backend');
  await page.getByLabel('Years of relevant experience').fill('0');
  await page.getByLabel('Technology', { exact: true }).fill('Java');
  await page.getByRole('button', { name: 'Add technology', exact: true }).click();
  await page.getByLabel('Country of residence').fill('Spain');
  await page.getByLabel('City of residence').fill('Madrid');
  await page.getByLabel('Preferred work mode').selectOption(mode);
}

test('saves, reloads, edits and clears a profile', async ({ page }, testInfo) => {
  await page.goto('/#profile');
  await fillProfile(page, 'hybrid');
  await page.getByLabel('Maximum on-site days per week').selectOption('2');
  await page.getByRole('button', { name: 'Save profile', exact: true }).click();
  await expect(page.getByRole('status', { name: 'Profile updates' })).toHaveText('Profile saved in this browser.');
  const summary = page.getByRole('complementary', { name: 'Saved profile' });
  await expect(summary).toContainText('Madrid, Spain');
  await expect(summary).toContainText('0 years');
  await expect(summary).toContainText('2 per week');
  await page.reload();
  await expect(page.getByLabel('Years of relevant experience')).toHaveValue('0');
  await expect(page.getByLabel('Maximum on-site days per week')).toHaveValue('2');
  await page.getByLabel('City of residence').fill('Valencia');
  await expect(summary).toContainText('Madrid, Spain');
  await expect(page.getByText('You have unsaved changes.')).toBeVisible();
  await page.reload();
  await expect(page.getByLabel('City of residence')).toHaveValue('Madrid');
  await page.getByLabel('City of residence').fill('Valencia');
  await page.getByRole('button', { name: 'Save profile', exact: true }).click();
  await expect(summary).toContainText('Valencia, Spain');
  expect(await page.evaluate(() => document.documentElement.scrollWidth > window.innerWidth)).toBe(false);
  await page.screenshot({ path: testInfo.outputPath('profile.png'), fullPage: true });

  page.once('dialog', (dialog) => dialog.dismiss());
  await page.getByRole('button', { name: 'Clear saved profile' }).click();
  await expect(summary).toContainText('Valencia, Spain');
  page.once('dialog', (dialog) => dialog.accept());
  await page.getByRole('button', { name: 'Clear saved profile' }).click();
  await expect(page.getByRole('status', { name: 'Profile updates' })).toHaveText('Profile cleared from this browser.');
  await page.reload();
  await expect(page.getByLabel('Target role', { exact: true })).toHaveValue('');
  await expect(summary).toContainText('No saved profile yet');
});

test('validates required fields, whole years and conditional days with focused errors', async ({ page }) => {
  await page.goto('/#profile');
  await page.getByRole('button', { name: 'Save profile', exact: true }).click();
  await expect(page.getByLabel('Target role', { exact: true })).toBeFocused();
  await expect(page.getByText('Add at least one technology.')).toBeVisible();
  expect(await page.evaluate((key) => localStorage.getItem(key), PROFILE_STORAGE_KEY)).toBeNull();
  await fillProfile(page, 'hybrid');
  await page.getByLabel('Years of relevant experience').fill('-1');
  await page.getByRole('button', { name: 'Save profile', exact: true }).click();
  await expect(page.getByLabel('Years of relevant experience')).toBeFocused();
  await expect(page.getByText('Enter a whole number of years from 0 to 60.')).toBeVisible();
  await page.getByLabel('Years of relevant experience').fill('0');
  await page.getByRole('button', { name: 'Save profile', exact: true }).click();
  await expect(page.getByLabel('Maximum on-site days per week')).toBeFocused();
  await page.getByLabel('Maximum on-site days per week').selectOption('5');
  await page.getByLabel('Preferred work mode').selectOption('remote');
  await expect(page.getByLabel('Maximum on-site days per week')).toHaveCount(0);
  await page.getByRole('button', { name: 'Save profile', exact: true }).click();
  await expect(page.getByRole('complementary', { name: 'Saved profile' })).toContainText('None — remote only');
  await page.reload();
  await expect(page.getByLabel('Preferred work mode')).toHaveValue('remote');
  await page.getByLabel('Preferred work mode').selectOption('onsite');
  await expect(page.getByLabel('Maximum on-site days per week')).toHaveValue('');
  await page.getByLabel('Maximum on-site days per week').selectOption('5');
  await page.getByRole('button', { name: 'Save profile', exact: true }).click();
  await page.reload();
  await expect(page.getByLabel('Preferred work mode')).toHaveValue('onsite');
  await expect(page.getByLabel('Maximum on-site days per week')).toHaveValue('5');
});

test('handles technologies through the keyboard and saves pending input', async ({ page }) => {
  await page.goto('/#profile');
  await fillProfile(page);
  const technology = page.getByLabel('Technology', { exact: true });
  await technology.fill(' java ');
  await technology.press('Enter');
  await expect(page.getByText('This technology is already in your profile.')).toBeVisible();
  await expect(page.getByRole('list', { name: 'Selected technologies' }).getByRole('listitem')).toHaveCount(1);
  await technology.fill('TypeScript');
  await technology.press('Enter');
  await page.getByRole('button', { name: 'Remove Java', exact: true }).focus();
  await page.keyboard.press('Enter');
  await expect(technology).toBeFocused();
  await technology.fill(' React ');
  await page.getByRole('button', { name: 'Save profile', exact: true }).click();
  await page.reload();
  const summary = page.getByRole('complementary', { name: 'Saved profile' });
  await expect(summary).toContainText('TypeScript');
  await expect(summary).toContainText('React');
  await expect(page.getByRole('button', { name: 'Remove Java', exact: true })).toHaveCount(0);
});

test('completes the form using keyboard navigation only', async ({ page }) => {
  await page.goto('/#profile');
  await page.getByLabel('Target role', { exact: true }).focus();
  await page.keyboard.press('b');
  await page.keyboard.press('Tab');
  await page.keyboard.type('0');
  await page.keyboard.press('Tab');
  await page.keyboard.type('Java');
  await page.keyboard.press('Enter');
  // Skip Add technology and Remove Java to reach the residence inputs.
  await page.keyboard.press('Tab');
  await page.keyboard.press('Tab');
  await page.keyboard.press('Tab');
  await expect(page.getByLabel('Country of residence')).toBeFocused();
  await page.keyboard.type('Spain');
  await page.keyboard.press('Tab');
  await page.keyboard.type('Madrid');
  await page.keyboard.press('Tab');
  await page.keyboard.press('r');
  await page.keyboard.press('Tab');
  await expect(page.getByRole('button', { name: 'Save profile', exact: true })).toBeFocused();
  await page.keyboard.press('Enter');
  await expect(page.getByRole('status', { name: 'Profile updates' })).toHaveText('Profile saved in this browser.');
});

for (const savedData of ['{invalid', '{"version":2,"profile":{}}', '{"version":1,"profile":{"role":"backend"}}']) {
  test('recovers from unreadable saved data: ' + savedData, async ({ page }) => {
    await page.goto('/#profile');
    await page.evaluate(({ key, raw }) => localStorage.setItem(key, raw), { key: PROFILE_STORAGE_KEY, raw: savedData });
    await page.reload();
    await expect(page.getByText(/Saved profile data could not be read/)).toBeVisible();
    await fillProfile(page);
    await page.getByRole('button', { name: 'Save profile', exact: true }).click();
    await expect(page.getByRole('status', { name: 'Profile updates' })).toHaveText('Profile saved in this browser.');
    await page.reload();
    await expect(page.getByText(/Saved profile data could not be read/)).toHaveCount(0);
    await expect(page.getByRole('complementary', { name: 'Saved profile' })).toContainText('Madrid, Spain');
  });
}

test('keeps edits and the last saved summary when writing or clearing fails', async ({ page }) => {
  await page.goto('/#profile');
  await fillProfile(page);
  await page.getByRole('button', { name: 'Save profile', exact: true }).click();
  await page.evaluate(() => {
    Storage.prototype.setItem = () => { throw new DOMException('Full', 'QuotaExceededError'); };
    Storage.prototype.removeItem = () => { throw new DOMException('Blocked', 'SecurityError'); };
  });
  await page.getByLabel('City of residence').fill('Bilbao');
  await page.getByRole('button', { name: 'Save profile', exact: true }).click();
  await expect(page.getByText(/Your profile could not be saved/)).toBeVisible();
  await expect(page.getByRole('status', { name: 'Profile updates' })).toBeEmpty();
  await expect(page.getByLabel('City of residence')).toHaveValue('Bilbao');
  await expect(page.getByRole('complementary', { name: 'Saved profile' })).toContainText('Madrid, Spain');
  page.once('dialog', (dialog) => dialog.accept());
  await page.getByRole('button', { name: 'Clear saved profile' }).click();
  await expect(page.getByText(/The saved profile could not be cleared/)).toBeVisible();
  await page.reload();
  await expect(page.getByLabel('City of residence')).toHaveValue('Madrid');
});

test('renders an editable form when access to storage is blocked', async ({ page }) => {
  await page.addInitScript(() => {
    Object.defineProperty(window, 'localStorage', {
      configurable: true, get() { throw new DOMException('Blocked', 'SecurityError'); },
    });
  });
  await page.goto('/#profile');
  await expect(page.getByText(/Browser storage is unavailable/)).toBeVisible();
  await fillProfile(page);
  await page.getByRole('button', { name: 'Save profile', exact: true }).click();
  await expect(page.getByText(/Your profile could not be saved/)).toBeVisible();
  await expect(page.getByLabel('City of residence')).toHaveValue('Madrid');
});

test('works during an API outage without transmitting profile fields', async ({ page }) => {
  const apiRequests: string[] = [];
  page.on('request', (request) => {
    if (new URL(request.url()).pathname.startsWith('/api/')) apiRequests.push(new URL(request.url()).pathname);
  });
  await page.route('**/api/status', (route) => route.fulfill({ status: 503, body: '{}' }));
  await page.goto('/#profile');
  await fillProfile(page);
  await page.getByRole('button', { name: 'Save profile', exact: true }).click();
  await expect(page.getByRole('status', { name: 'Profile updates' })).toHaveText('Profile saved in this browser.');
  await page.getByRole('link', { name: 'Service status', exact: true }).click();
  await expect(page.getByRole('status', { name: 'API connection' })).toContainText('The API is unavailable');
  await page.getByRole('navigation', { name: 'Main navigation' }).getByRole('link', { name: 'Profile', exact: true }).click();
  await expect(page.getByRole('status', { name: 'Profile updates' })).toHaveText('Profile saved in this browser.');
  expect(apiRequests.length).toBeGreaterThan(0);
  expect(apiRequests.every((path) => path === '/api/status')).toBe(true);
});

test('keeps a narrow viewport usable with long values and twenty technologies', async ({ page }) => {
  await page.setViewportSize({ width: 320, height: 740 });
  await page.goto('/#profile');
  await fillProfile(page);
  const technology = page.getByLabel('Technology', { exact: true });
  for (let i = 1; i < 20; i++) {
    await technology.fill(String(i).padEnd(40, 'X'));
    await technology.press('Enter');
  }
  await technology.fill('One more');
  await technology.press('Enter');
  await expect(page.getByText('Add up to 20 technologies.', { exact: true })).toBeVisible();
  await technology.fill('');
  await page.getByLabel('City of residence').fill('C'.repeat(80));
  await page.getByRole('button', { name: 'Save profile', exact: true }).click();
  await expect(page.getByRole('status', { name: 'Profile updates' })).toHaveText('Profile saved in this browser.');
  expect(await page.evaluate(() => document.documentElement.scrollWidth > window.innerWidth)).toBe(false);
});
