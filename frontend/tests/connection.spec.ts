import { expect, test } from '@playwright/test';

test('connects through the frontend proxy to the real Java API', async ({ page }) => {
  await page.goto('/');
  await expect(page.getByRole('heading', { level: 1 })).toContainText('Good work starts');
  await expect(page.getByRole('status', { name: 'API connection' })).toHaveText('Connected to the JobSinton API');
  await expect(page.getByText('Planned capabilities')).toBeVisible();

  // The first tab must expose a working skip link for keyboard navigation.
  await page.keyboard.press('Tab');
  await expect(page.getByRole('link', { name: 'Skip to content' })).toBeFocused();
  await page.keyboard.press('Enter');
  await expect(page).toHaveURL(/#main$/);

  const overflows = await page.evaluate(
    () => document.documentElement.scrollWidth > window.innerWidth,
  );
  expect(overflows).toBe(false);
});

test('explains an outage and reconnects when the service returns', async ({ page }) => {
  await page.route('**/api/status', (route) => route.fulfill({
    status: 503, contentType: 'application/json', body: '{}',
  }));
  await page.goto('/');
  await expect(page.getByRole('status', { name: 'API connection' })).toContainText('The API is unavailable');

  await page.unroute('**/api/status');
  await page.getByRole('button', { name: 'Check connection again' }).click();
  await expect(page.getByRole('status', { name: 'API connection' })).toHaveText('Connected to the JobSinton API');
});

test('does not report malformed API responses as a successful connection', async ({ page }) => {
  await page.route('**/api/status', (route) => route.fulfill({
    status: 200, contentType: 'application/json', body: '{"status":"UP"}',
  }));
  await page.goto('/');
  await expect(page.getByRole('status', { name: 'API connection' })).toContainText('The API is unavailable');
});

test('bounds a stalled request and permits another attempt', async ({ page }) => {
  // Keep the first response pending beyond the application timeout.
  await page.route('**/api/status', async (route) => {
    await new Promise((resolve) => setTimeout(resolve, 6500));
    await route.fulfill({
      status: 200, contentType: 'application/json',
      body: '{"name":"JobSinton","status":"UP"}',
    }).catch(() => { /* The client may already have cancelled this request. */ });
  });
  await page.goto('/');
  await expect(page.getByRole('status', { name: 'API connection' })).toContainText('Checking the connection');
  await expect(page.getByRole('button', { name: 'Checking…' })).toBeDisabled();
  await expect(page.getByRole('status', { name: 'API connection' })).toContainText('The API is unavailable', { timeout: 8000 });
  await expect(page.getByRole('button', { name: 'Check connection again' })).toBeEnabled();
});
