import { expect, test, type Page } from '@playwright/test';

/** Pause only page fades so their intermediate state can be inspected without timing races. */
async function inspectFades(page: Page) {
  await page.addInitScript(() => {
    const animate = Element.prototype.animate;
    Element.prototype.animate = function (this: Element, keyframes, options) {
      const animation = animate.call(this, keyframes, options);
      if (this.id === 'main') animation.pause();
      return animation;
    };
  });
}
async function activeFades(page: Page) {
  return page.locator('#main').evaluate(el => el.getAnimations().length);
}
async function finishFade(page: Page) {
  await page.locator('#main').evaluate(el => el.getAnimations().forEach(animation => animation.finish()));
}

test('keyboard navigation fades only incoming content and completes at full opacity', async ({ page }) => {
  await page.emulateMedia({ reducedMotion: 'no-preference' });
  await inspectFades(page);
  await page.goto('/#home');
  expect(await activeFades(page)).toBe(0);
  const header = await page.locator('.site-header').boundingBox();
  await page.locator('.site-nav a[href="#companies"]').focus();
  await page.keyboard.press('Enter');
  await expect(page.getByRole('heading', { name: 'Find your kind of team.' })).toBeAttached();
  await expect(page.locator('#main')).toBeFocused();
  await expect.poll(() => activeFades(page)).toBe(1);
  const midpoint = await page.locator('#main').evaluate(async el => {
    const animation = el.getAnimations()[0];
    await animation.ready;
    animation.currentTime = Number(animation.effect!.getTiming().duration) / 2;
    return Number(getComputedStyle(el).opacity);
  });
  expect(midpoint).toBeGreaterThan(0);
  expect(midpoint).toBeLessThan(1);
  await expect(page.locator('.site-header')).toHaveCSS('opacity', '1');
  await expect(page.locator('.page-footer')).toHaveCSS('opacity', '1');
  expect(await page.locator('.site-header').boundingBox()).toEqual(header);
  expect(await page.locator('.site-header').evaluate(el => el.getAnimations({ subtree: true }).length)).toBe(0);
  await page.locator('#main').evaluate(el => el.getAnimations()[0].play());
  await expect(page.locator('#main')).toHaveCSS('opacity', '1');
  await expect.poll(() => activeFades(page)).toBe(0);
  expect(await page.evaluate(() => scrollY)).toBe(0);
});

test('reduced motion disables fades and immediately cancels an active transition', async ({ page }) => {
  await inspectFades(page);
  await page.emulateMedia({ reducedMotion: 'reduce' });
  await page.goto('/#home');
  await page.locator('.site-nav a[href="#companies"]').click();
  await expect(page.getByRole('heading', { name: 'Find your kind of team.' })).toBeVisible();
  expect(await activeFades(page)).toBe(0);
  await expect(page.locator('#main')).toHaveCSS('opacity', '1');
  await page.emulateMedia({ reducedMotion: 'no-preference' });
  await page.locator('.site-nav a[href="#saved"]').click();
  await expect.poll(() => activeFades(page)).toBe(1);
  await page.emulateMedia({ reducedMotion: 'reduce' });
  await expect.poll(() => activeFades(page)).toBe(0);
  await expect(page.locator('#main')).toHaveCSS('opacity', '1');
  await page.locator('.site-nav a[href="#profile"]').click();
  await expect(page.getByLabel('City of residence')).toBeVisible();
  expect(await activeFades(page)).toBe(0);
});

test('rapid navigation and history keep one transition and preserve the profile draft', async ({ page }) => {
  await page.emulateMedia({ reducedMotion: 'no-preference' });
  await inspectFades(page);
  await page.goto('/#profile');
  await page.getByLabel('City of residence').fill('Valencia');
  await page.locator('.site-nav a[href="#companies"]').click();
  await expect.poll(() => activeFades(page)).toBe(1);
  // A paused fade must not block a newer route, including browser Back and Forward.
  await page.locator('.site-nav a[href="#saved"]').click();
  await expect(page.getByRole('heading', { name: 'Worth coming back to.' })).toBeAttached();
  expect(await activeFades(page)).toBe(1);
  await page.goBack();
  await expect(page.getByRole('heading', { name: 'Find your kind of team.' })).toBeAttached();
  expect(await activeFades(page)).toBe(1);
  await page.goBack();
  await expect(page.getByRole('heading', { name: 'A profile with purpose.' })).toBeAttached();
  await expect(page.getByLabel('City of residence')).toHaveValue('Valencia');
  expect(await activeFades(page)).toBe(1);
  await finishFade(page);
  await page.getByLabel('City of residence').fill('Bilbao');
  expect(await activeFades(page)).toBe(0);
  await page.goForward();
  await expect(page.getByRole('heading', { name: 'Find your kind of team.' })).toBeAttached();
  expect(await activeFades(page)).toBe(1);
  await finishFade(page);
  await expect(page.locator('#main')).toHaveCSS('opacity', '1');
});

test('filters, layout, saved actions and in-page anchors do not restart the fade', async ({ page }) => {
  await page.emulateMedia({ reducedMotion: 'no-preference' });
  await inspectFades(page);
  await page.goto('/#opportunities');
  await page.getByLabel('Search opportunities').fill('java');
  await page.getByRole('button', { name: 'Card view', exact: true }).click();
  await page.getByRole('button', { name: 'Save Backend Developer', exact: true }).click();
  expect(await activeFades(page)).toBe(0);
  await page.locator('.site-nav a[href="#opportunities"]').click();
  await expect(page.locator('.job')).toHaveCount(6);
  expect(await activeFades(page)).toBe(0);
  await page.getByRole('link', { name: 'Backend Developer', exact: true }).click();
  await expect(page.getByRole('heading', { level: 1 })).toHaveText('Backend Developer.');
  expect(await activeFades(page)).toBe(1);
  await finishFade(page);
  await page.getByRole('button', { name: 'Apply for this role' }).click();
  await expect(page.getByRole('dialog')).toBeVisible();
  await page.keyboard.press('Escape');
  await expect(page.getByRole('button', { name: 'Apply for this role' })).toBeFocused();
  expect(await activeFades(page)).toBe(0);
  await page.locator('.company-lockup a').click();
  await expect(page.getByRole('heading', { level: 1 })).toHaveText('Northline Studio.');
  await finishFade(page);
  await page.getByRole('link', { name: 'Explore open roles' }).click();
  await expect(page.locator('#open-roles')).toBeFocused();
  expect(await activeFades(page)).toBe(0);
});

test('browsers without the animation API retain usable navigation', async ({ page }) => {
  const errors: string[] = [];
  page.on('pageerror', error => errors.push(error.message));
  await page.addInitScript(() => Object.defineProperty(Element.prototype, 'animate', { value: undefined, configurable: true }));
  await page.goto('/#home');
  await page.locator('.site-nav a[href="#profile"]').click();
  await expect(page.getByLabel('City of residence')).toBeVisible();
  await page.getByLabel('City of residence').fill('Madrid');
  await expect(page.locator('#main')).toHaveCSS('opacity', '1');
  await page.locator('.site-nav a[href="#companies"]').click();
  await expect(page.getByRole('heading', { name: 'Find your kind of team.' })).toBeVisible();
  expect(errors).toEqual([]);
});
