import { expect, test, type Page } from '@playwright/test';

const MOCK_FEED = {
  fetchedAt: '2026-11-17T12:00:00Z',
  cached: false,
  cacheAgeSeconds: 0,
  totalBoards: 7,
  succeededBoards: 6,
  failedBoards: 1,
  boards: [
    { companySlug: 'cabify', companyName: 'Cabify', provider: 'greenhouse', providerLabel: 'Greenhouse', ok: true, jobCount: 1, durationMs: 145, error: null },
    { companySlug: 'paradigmadigital', companyName: 'Paradigma Digital', provider: 'personio', providerLabel: 'Personio', ok: true, jobCount: 1, durationMs: 160, error: null },
    { companySlug: 'tomtom', companyName: 'TomTom', provider: 'lever', providerLabel: 'Lever', ok: true, jobCount: 1, durationMs: 210, error: null },
    { companySlug: 'Celonis', companyName: 'Celonis', provider: 'smartrecruiters', providerLabel: 'SmartRecruiters', ok: true, jobCount: 1, durationMs: 260, error: null },
    { companySlug: 'mediaradar', companyName: 'MediaRadar', provider: 'workable', providerLabel: 'Workable', ok: true, jobCount: 1, durationMs: 185, error: null },
    { companySlug: 'invopop', companyName: 'Invopop', provider: 'ashby', providerLabel: 'Ashby', ok: true, jobCount: 1, durationMs: 130, error: null },
    { companySlug: 'broken-board', companyName: 'Broken Co', provider: 'ashby', providerLabel: 'Ashby', ok: false, jobCount: 0, durationMs: 8000, error: 'Timed out after 8000ms' },
  ],
  jobs: [
    {
      id: 'greenhouse:cabify:101',
      sourceId: '101',
      provider: 'greenhouse',
      providerLabel: 'Greenhouse',
      companySlug: 'cabify',
      companyName: 'Cabify',
      title: 'Full Stack Engineer (Java Spring Boot & Angular)',
      location: 'Madrid, Spain',
      workplaceType: 'hybrid',
      publishedAt: '2026-11-17T08:00:00Z',
      hoursSincePublished: 4,
      description: 'Build scalable mobility platforms using Java 21, Spring Boot, Angular, PostgreSQL and Docker in Madrid.',
      descriptionSnippet: 'Build scalable mobility platforms using Java 21, Spring Boot, Angular, PostgreSQL and Docker in Madrid.',
      applyUrl: 'https://boards.greenhouse.io/cabify/jobs/101',
      department: 'Engineering',
      compensation: null,
      hasFullDescription: true,
    },
    {
      id: 'ashby:invopop:ash-1',
      sourceId: 'ash-1',
      provider: 'ashby',
      providerLabel: 'Ashby',
      companySlug: 'invopop',
      companyName: 'Invopop',
      title: 'Product Engineer (React, TypeScript & Node.js)',
      location: 'Remote - Europe (CET)',
      workplaceType: 'remote',
      publishedAt: '2026-11-16T06:00:00Z',
      hoursSincePublished: 30,
      description: 'Modern invoicing API built with TypeScript, React, Node.js, PostgreSQL and Docker.',
      descriptionSnippet: 'Modern invoicing API built with TypeScript, React, Node.js, PostgreSQL and Docker.',
      applyUrl: 'https://jobs.ashbyhq.com/invopop/ash-1/application',
      department: 'Product',
      compensation: '€45K – €58K',
      hasFullDescription: true,
    },
    {
      id: 'smartrecruiters:Celonis:sr-sap',
      sourceId: 'sr-sap',
      provider: 'smartrecruiters',
      providerLabel: 'SmartRecruiters',
      companySlug: 'Celonis',
      companyName: 'Celonis',
      title: 'Junior SAP ABAP Developer',
      location: 'Madrid, Spain, Remote',
      workplaceType: 'remote',
      publishedAt: '2026-11-16T18:00:00Z',
      hoursSincePublished: 18,
      description: '',
      descriptionSnippet: '',
      applyUrl: 'https://jobs.smartrecruiters.com/Celonis/sr-sap',
      department: 'Enterprise Engineering',
      compensation: null,
      hasFullDescription: false,
    },
    {
      id: 'personio:paradigmadigital:p-1',
      sourceId: 'p-1',
      provider: 'personio',
      providerLabel: 'Personio',
      companySlug: 'paradigmadigital',
      companyName: 'Paradigma Digital',
      title: 'Junior Java Backend Developer',
      location: 'Madrid, Spain',
      workplaceType: 'hybrid',
      publishedAt: '2026-11-17T06:00:00Z',
      hoursSincePublished: 6,
      description: 'Java 21, Spring Boot, Docker and PostgreSQL in Madrid.',
      descriptionSnippet: 'Java 21, Spring Boot, Docker and PostgreSQL in Madrid.',
      applyUrl: 'https://paradigmadigital.jobs.personio.com/job/p-1',
      department: 'Software',
      compensation: null,
      hasFullDescription: true,
    },
    {
      id: 'lever:tomtom:lev-us-fake',
      sourceId: 'lev-us-fake',
      provider: 'lever',
      providerLabel: 'Lever',
      companySlug: 'tomtom',
      companyName: 'TomTom',
      title: 'Backend Software Engineer (Remote - US Only)',
      location: 'Remote - United States',
      workplaceType: 'remote',
      publishedAt: '2026-11-10T12:00:00Z',
      hoursSincePublished: 168,
      description: 'Distributed navigation backend using Java, Kubernetes and Docker. Must reside in the US.',
      descriptionSnippet: 'Distributed navigation backend using Java, Kubernetes and Docker. Must reside in the US.',
      applyUrl: 'https://jobs.eu.lever.co/tomtom/lev-us-fake/apply',
      department: 'Maps',
      compensation: null,
      hasFullDescription: true,
    },
    {
      id: 'workable:mediaradar:wk-9',
      sourceId: 'wk-9',
      provider: 'workable',
      providerLabel: 'Workable',
      companySlug: 'mediaradar',
      companyName: 'MediaRadar',
      title: 'Account Executive EMEA',
      location: 'London, United Kingdom',
      workplaceType: 'hybrid',
      publishedAt: '2026-11-12T12:00:00Z',
      hoursSincePublished: 120,
      description: 'Enterprise B2B sales role with strong capacity for SaaS pipeline growth.',
      descriptionSnippet: 'Enterprise B2B sales role with strong capacity for SaaS pipeline growth.',
      applyUrl: 'https://apply.workable.com/mediaradar/j/wk-9/',
      department: 'Sales',
      compensation: null,
      hasFullDescription: true,
    },
  ],
};

async function mockAtsApi(page: Page, onJobsRequest?: (url: string) => void) {
  await page.route('**/api/ats/jobs*', async (route) => {
    onJobsRequest?.(route.request().url());
    await route.fulfill({
      status: 200,
      contentType: 'application/json; charset=utf-8',
      body: JSON.stringify(MOCK_FEED),
    });
  });
  await page.route('**/api/ats/smartrecruiters/Celonis/sr-sap', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json; charset=utf-8',
      body: JSON.stringify({
        id: 'smartrecruiters:Celonis:sr-sap',
        description: 'SAP S/4HANA clean core development using ABAP OO, RAP, CAP, OData v4, Fiori and SAP BTP.',
        descriptionSnippet: 'SAP S/4HANA clean core development using ABAP OO, RAP, CAP, OData v4, Fiori and SAP BTP.',
        applyUrl: 'https://jobs.smartrecruiters.com/Celonis/sr-sap/apply',
        hasFullDescription: true,
      }),
    });
  });
  await page.route('**/api/ats/discover/url', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json; charset=utf-8',
      body: JSON.stringify({
        discoveredCount: 1,
        discovered: [
          {
            board: { slug: 'marsbased', name: 'MarsBased', ats: 'teamtailor' },
            jobCount: 3,
            newlyAdded: true,
            sampleJobs: [],
          },
        ],
        message: 'Discovered 1 live ATS board(s).',
      }),
    });
  });
}

test('zero-friction live ATS feed defaults to Spain + EU Remote (blocking Fake US Remote), supports 3-tier geo, triage, and discovery', async ({ page }) => {
  const requestedUrls: string[] = [];
  await mockAtsApi(page, (url) => requestedUrls.push(url));

  await page.goto('/#opportunities');

  // Defaults straight to Live ATS Boards + Spain + EU Remote scope.
  await expect(page.getByRole('button', { name: 'Live ATS Boards', exact: true })).toHaveAttribute('aria-pressed', 'true');
  await expect(page.getByRole('button', { name: /🇪🇺 Spain \+ EU Remote/ })).toHaveAttribute('aria-pressed', 'true');
  await expect(page.getByText(/6 \/ 7 company boards live/)).toBeVisible();

  // Default Spain + EU Remote + All Tech preset shows the 4 Spain/EU tech roles and blocks the Fake US Remote role + non-tech London Sales role.
  await expect(page.locator('[data-ats-job]')).toHaveCount(4);
  await expect(page.locator('[data-ats-job="lever:tomtom:lev-us-fake"]')).toHaveCount(0);

  // Switch to 🇪🇸 Spain Only -> filters out the EU-only Remote Invopop role, leaving the 3 Spain roles.
  await page.getByRole('button', { name: /🇪🇸 Spain Only/ }).click();
  await expect(page.locator('[data-ats-job]')).toHaveCount(3);

  // Switch back to 🇪🇺 Spain + EU Remote -> 4 roles.
  await page.getByRole('button', { name: /🇪🇺 Spain \+ EU Remote/ }).click();
  await expect(page.locator('[data-ats-job]')).toHaveCount(4);

  // Junior fit badge renders on Junior roles.
  const paradigmaCard = page.locator('[data-ats-job="personio:paradigmadigital:p-1"]');
  await expect(paradigmaCard).toContainText('🎯 Junior / Entry Fit');
  await expect(paradigmaCard).toContainText('Personio');

  // 1-Click Triage: mark Paradigma Digital as "✅ Applied" -> automatically hidden while "Hide Applied & Dismissed" is active.
  await paradigmaCard.getByRole('button', { name: '✅ Applied', exact: true }).click();
  await expect(page.locator('[data-ats-job="personio:paradigmadigital:p-1"]')).toHaveCount(0);
  await page.getByRole('button', { name: /View Applied/ }).click();
  await expect(page.locator('[data-ats-job="personio:paradigmadigital:p-1"]')).toBeVisible();
  // Un-apply to restore normal list.
  await page.locator('[data-ats-job="personio:paradigmadigital:p-1"]').getByRole('button', { name: '✅ Applied', exact: true }).click();
  await page.getByRole('button', { name: /View Applied/ }).click();

  // Switch to Track A (SAP & Enterprise) and hydrate SmartRecruiters description on demand.
  await page.getByRole('button', { name: /Track A · SAP & Enterprise/ }).click();
  await expect(page.locator('[data-ats-job]')).toHaveCount(1);
  const celonisCard = page.locator('[data-ats-job="smartrecruiters:Celonis:sr-sap"]');
  await celonisCard.getByRole('button', { name: 'Load full description', exact: true }).click();
  await expect(celonisCard).toContainText('SAP S/4HANA clean core development');
  await expect(celonisCard.locator('.ats-match-chip', { hasText: 'OData' })).toBeVisible();

  // Switch to All Fetched Jobs (Global) to reveal all 6 postings including US Fake Remote and Sales.
  await page.getByRole('button', { name: /All Fetched Jobs/ }).click();
  await expect(page.locator('[data-ats-job]')).toHaveCount(6);

  // Test the Blip Auto-Discovery Panel (URL Sniffer).
  await page.getByRole('button', { name: '➕ Discover / Add Companies', exact: true }).click();
  await page.getByLabel('Career or job board URL').fill('https://marsbased.teamtailor.com/jobs');
  await page.getByRole('button', { name: 'Sniff Career URL', exact: true }).click();
  await expect(page.getByText('MarsBased (teamtailor · 3 jobs)')).toBeVisible();

  // Force Refresh calls /api/ats/jobs?refresh=true explicitly.
  await page.getByRole('button', { name: 'Force Refresh', exact: true }).click();
  await expect.poll(() => requestedUrls.some((url) => url.includes('refresh=true'))).toBe(true);
});

test('home page renders Live ATS Radar preview, supports ES/EN switch, and fits 320px mobile viewports', async ({ page }) => {
  await mockAtsApi(page);
  const errors: string[] = [];
  page.on('pageerror', (err) => errors.push(err.message));

  await page.goto('/#home');
  await expect(page.getByRole('heading', { name: 'Fresh directly from company career pages.' })).toBeVisible();
  await expect(page.locator('.ats-preview-card')).toHaveCount(4);

  // Shortcut pill jumps straight into Track A · SAP on Opportunities.
  await page.locator('.home-preset-shortcuts a', { hasText: 'Track A · SAP & Enterprise' }).click();
  await expect(page.getByRole('button', { name: /Track A · SAP & Enterprise/ })).toHaveAttribute('aria-pressed', 'true');
  await expect(page.locator('[data-ats-job="smartrecruiters:Celonis:sr-sap"]')).toBeVisible();

  // Switch language to Spanish in place.
  await page.getByRole('button', { name: 'Español', exact: true }).click();
  await expect(page.getByRole('button', { name: 'Portales ATS en directo', exact: true })).toHaveAttribute('aria-pressed', 'true');
  await expect(page.getByText(/portales de empresa activos/)).toBeVisible();
  await expect(page.getByRole('button', { name: /🇪🇺 España \+ Remoto UE/ })).toBeVisible();

  // Verify narrow mobile viewport has zero horizontal overflow.
  await page.setViewportSize({ width: 320, height: 800 });
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= window.innerWidth)).toBe(true);
  expect(errors).toEqual([]);
});
