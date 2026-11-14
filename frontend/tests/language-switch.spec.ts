import { expect, test } from '@playwright/test';

test.use({ locale: 'es-ES' });

test('Spanish first visit, accessible selector and saved choice across reload', async ({ page }) => {
  await page.goto('/#opportunities');
  await expect(page.locator('html')).toHaveAttribute('lang','es');
  await expect(page.getByRole('heading',{name:'Ofertas.',exact:true})).toBeVisible();
  await page.getByLabel('Buscar ofertas',{exact:true}).fill('desarrollo backend');
  await expect(page.locator('.job')).toHaveCount(1);
  await page.getByRole('button',{name:'English',exact:true}).click();
  await expect(page.getByLabel('Search opportunities',{exact:true})).toHaveValue('desarrollo backend');
  await expect(page.locator('.job')).toHaveCount(1);
  await page.reload();
  await expect(page.locator('html')).toHaveAttribute('lang','en');
  await expect(page.getByRole('button',{name:'English',exact:true})).toHaveAttribute('aria-pressed','true');
});

test('profile draft and error survive switching without navigation', async ({ page }) => {
  await page.goto('/#profile');
  await page.getByLabel('Ciudad de residencia',{exact:true}).fill('A Coruña');
  await page.getByRole('button',{name:'Guardar perfil',exact:true}).click();
  await expect(page.getByText('Elige un puesto deseado.',{exact:true})).toBeVisible();
  await page.getByRole('button',{name:'English',exact:true}).click();
  await expect(page.getByLabel('City of residence',{exact:true})).toHaveValue('A Coruña');
  await expect(page.getByText('Choose a target role.',{exact:true})).toBeVisible();
  await expect(page).toHaveURL(/#profile$/);
});

test('fictional detail and matching reasons switch without another evaluation', async ({ page }) => {
  await page.addInitScript(() => localStorage.setItem('jobsinton.professional-profile', JSON.stringify({
    version: 1, profile: {role:'backend', technologies:['Java'], experienceYears:2, city:'Madrid',country:'Spain',workMode:'remote',maxOnsiteDays:null}
  })));
  let calls = 0;
  page.on('request', r => { if(r.url().endsWith('/api/matching/evaluate')) calls++; });
  await page.goto('/#opportunities/backend-java');
  await expect(page.getByRole('heading',{name:'Desarrollo backend',exact:true})).toBeVisible();
  await expect(page.getByText(/El puesto coincide con el objetivo del perfil/)).toBeVisible();
  const before = calls;
  await page.getByRole('button',{name:'English',exact:true}).click();
  await expect(page.getByText(/The opportunity matches the target role/)).toBeVisible();
  expect(calls).toBe(before);
});
