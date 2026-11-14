import { test, expect } from '@playwright/test';

const endpoint = '**/api/providers/adzuna/search';
const row = { id: 'adzuna:example', title: 'Java engineer', company: null, locationLabel: 'A Coruña',
  salary: null, excerpt: '¡España! Certificación para un equipo ficticio.', referral: 'https://www.adzuna.es/details/example' };
const pageOf = (rows: typeof row[]) => ({ source: 'Adzuna', page: 1, received: rows.length, duplicates: 0, rejected: 0, listings: rows });

test('real local page and API: only explicit submit reaches the credential-free backend', async ({ page }) => {
  const requests: string[] = [];
  page.on('request', r => { if (r.url().endsWith('/adzuna/search')) requests.push(r.method()); });
  await page.goto('/#opportunities');
  await expect(page.getByRole('button', { name: 'Sample catalog' })).toHaveAttribute('aria-pressed','true');
  await expect(page.getByRole('button', { name: 'Save Backend Developer', exact: true })).toBeVisible();
  await page.getByRole('button', { name: 'Adzuna · Spain' }).click();
  await page.getByLabel('Role or keyword').fill('Java');
  await page.getByLabel('Location in Spain').fill('A Coruña');
  expect(requests).toEqual([]);
  await page.getByRole('button', { name: 'Search Adzuna' }).click();
  await expect(page.getByRole('alert')).toContainText('credentials are missing');
  expect(requests).toEqual(['POST']);
});

test('renders partial live rows and full supplied text without demo assumptions or extra calls', async ({ page }) => {
  let calls = 0;
  await page.route(endpoint, async route => {
    calls++;
    expect(route.request().postDataJSON()).toEqual({ what: 'Java', where: 'Madrid', page: 1 });
    await route.fulfill({ json: pageOf([row]) });
  });
  await page.goto('/#opportunities');
  await page.getByRole('button', { name: 'Adzuna · Spain' }).click();
  await page.getByRole('button', { name: 'Search Adzuna' }).click();
  const live = page.getByRole('region',{name:'Adzuna Spain search'});
  await expect(live.getByRole('link',{name:row.title})).toHaveAttribute('href',row.referral);
  await expect(live.getByRole('link',{name:'Adzuna',exact:true})).toHaveAttribute('href','https://www.adzuna.es/');
  await expect(live.getByText('Salary: Not indicated')).toBeVisible();
  await expect(live.getByText('Company not indicated')).toBeVisible();
  await expect(live.getByText(/Work mode: not indicated/)).toBeVisible();
  await live.getByText('Read excerpt').click();
  await expect(live.getByText(row.excerpt)).toBeVisible();
  await expect(live.getByRole('button',{name:/save/i})).toHaveCount(0);
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= window.innerWidth)).toBeTruthy();
  await page.getByRole('button',{name:'Sample catalog'}).click();
  await expect(live).toHaveCount(0);
  expect(calls).toBe(1);
});

test('pagination is explicit and an error is distinct from zero results', async ({ page }) => {
  const requested: number[] = [];
  await page.route(endpoint, async route => {
    const { page: n } = route.request().postDataJSON();
    requested.push(n);
    if (requested.length === 1) return route.fulfill({ json: pageOf(Array.from({length:10}, (_,i)=>({...row,id:'adzuna:'+i}))) });
    if (requested.length === 2) return route.fulfill({ json: {...pageOf([]),page:2} });
    return route.fulfill({ status:429, json:{code:'PROVIDER_RATE_LIMITED'} });
  });
  await page.goto('/#opportunities');
  await page.getByRole('button',{name:'Adzuna · Spain'}).click();
  await page.getByRole('button',{name:'Search Adzuna'}).click();
  await expect(page.getByRole('button',{name:'Next page'})).toBeVisible();
  expect(requested).toEqual([1]);
  await page.getByRole('button',{name:'Next page'}).click();
  await expect(page.getByText(/No opportunities found.*Page 2/)).toBeVisible();
  await page.getByRole('button',{name:'Previous page'}).click();
  await expect(page.getByRole('alert')).toContainText('request limit');
  expect(requested).toEqual([1,2,1]);
});

test('switching language on page two preserves the advert, excerpt and request count', async ({ page }) => {
  const requested: number[] = [];
  await page.route(endpoint, route => {
    const n = route.request().postDataJSON().page;
    requested.push(n);
    const rows = n === 1 ? Array.from({length:10}, (_,i)=>({...row,id:'adzuna:'+i})) : [{...row,title:'Next page',excerpt:'Work mode'}];
    return route.fulfill({json:{...pageOf(rows),page:n}});
  });
  await page.goto('/#opportunities');
  await page.getByRole('button',{name:'Adzuna · Spain'}).click();
  await page.getByRole('button',{name:'Search Adzuna'}).click();
  await page.getByRole('button',{name:'Next page',exact:true}).click();
  await page.getByText('Read excerpt',{exact:true}).click();
  await page.getByRole('button',{name:'Español',exact:true}).click();
  await expect(page.getByText(/1 oferta.*Página 2/)).toBeVisible();
  await expect(page.getByRole('button',{name:'Página anterior'})).toBeVisible();
  await expect(page.getByRole('link',{name:'Next page',exact:true})).toBeVisible();
  await expect(page.locator('.live-job details[open] p')).toHaveText('Work mode');
  expect(requested).toEqual([1,2]);
});
