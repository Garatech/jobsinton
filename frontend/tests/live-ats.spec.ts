import { expect, test, type Page } from '@playwright/test';

const MOCK_FEED = {
  fetchedAt: '2026-11-17T12:00:00Z',
  cached: false,
  cacheAgeSeconds: 0,
  totalBoards: 6,
  succeededBoards: 5,
  failedBoards: 1,
  boards: [
    { companySlug: 'cabify', companyName: 'Cabify', provider: 'greenhouse', providerLabel: 'Greenhouse', ok: true, jobCount: 1, durationMs: 145, error: null },
    { companySlug: 'tomtom', companyName: 'TomTom', provider: 'lever', providerLabel: 'Lever', ok: true, jobCount: 2, durationMs: 210, error: null },
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
      location: 'Madrid, Spain · Remote EU',
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
      id: 'lever:tomtom:lev-1',
      sourceId: 'lev-1',
      provider: 'lever',
      providerLabel: 'Lever',
      companySlug: 'tomtom',
      companyName: 'TomTom',
      title: 'Backend Software Engineer',
      location: 'Remote - Europe (CET)',
      workplaceType: 'remote',
      publishedAt: '2026-11-10T12:00:00Z',
      hoursSincePublished: 168,
      description: 'Distributed navigation backend using Java, Kubernetes and Docker.',
      descriptionSnippet: 'Distributed navigation backend using Java, Kubernetes and Docker.',
      applyUrl: 'https://jobs.eu.lever.co/tomtom/lev-1/apply',
      department: 'Maps',
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
      title: 'Cloud Java Engineer (Remote - US Only)',
      location: 'Remote - US Only',
      workplaceType: 'remote',
      publishedAt: '2026-11-17T06:00:00Z',
      hoursSincePublished: 6,
      description: 'Must reside in the US and work EST/PST hours with Java and Docker.',
      descriptionSnippet: 'Must reside in the US and work EST/PST hours with Java and Docker.',
      applyUrl: 'https://jobs.lever.co/tomtom/lev-us-fake/apply',
      department: 'Cloud',
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
        discovered: [
          {
            slug: 'paradigmadigital',
            name: 'Paradigma Digital',
            provider: 'personio',
            providerLabel: 'Personio',
            apiEndpoint: 'https://paradigmadigital.jobs.personio.com/xml?language=es',
            jobCount: 8,
            newlyRegistered: true,
          },
        ],
        totalAdded: 1,
        totalJobsFound: 8,
        message: 'Detected 1 public ATS board(s) with 8 open role(s).',
      }),
    });
  });
}

test('zero-friction live ATS feed loads immediately with 3-tier geo filter, presets, freshness badges, triage, auto-discovery, and direct apply links', async ({ page }) => {
  const requestedUrls: string[] = [];
  await mockAtsApi(page, (url) => requestedUrls.push(url));

  await page.goto('/#opportunities');

  // Defaults straight to Live ATS Boards with Spain + EU Remote active by default.
  await expect(page.getByRole('button', { name: 'Live ATS Boards', exact: true })).toHaveAttribute('aria-pressed', 'true');
  await expect(page.getByText(/5 \/ 6 company boards live/)).toBeVisible();
  await expect(page.getByRole('button', { name: /🇪🇺 Spain \+ EU Remote/ })).toHaveAttribute('aria-pressed', 'true');

  // Default All Tech preset + Spain + EU Remote shows the 4 Spain/EU tech roles and blocks Fake US Remote + non-tech Sales.
  await expect(page.locator('[data-ats-job]')).toHaveCount(4);
  await expect(page.locator('[data-ats-job="lever:tomtom:lev-us-fake"]')).toHaveCount(0);

  // Switching to Spain Only narrows to the 3 Spain-based tech roles.
  await page.getByRole('button', { name: /🇪🇸 Spain Only/ }).click();
  await expect(page.locator('[data-ats-job]')).toHaveCount(3);
  await page.getByRole('button', { name: /🇪🇺 Spain \+ EU Remote/ }).click();
  await expect(page.locator('[data-ats-job]')).toHaveCount(4);

  // First-48h freshness badges and Ashby compensation render clearly.
  const cabifyCard = page.locator('[data-ats-job="greenhouse:cabify:101"]');
  await expect(cabifyCard).toContainText('🔥 Posted 4h ago');
  await expect(cabifyCard).toContainText('Greenhouse');
  await expect(cabifyCard.getByRole('link', { name: /Apply on Company Site/ })).toHaveAttribute(
    'href',
    'https://boards.greenhouse.io/cabify/jobs/101',
  );
  await expect(cabifyCard.getByRole('link', { name: /Apply on Company Site/ })).toHaveAttribute('target', '_blank');

  // Junior role displays Junior/Entry Fit badge.
  const celonisCard = page.locator('[data-ats-job="smartrecruiters:Celonis:sr-sap"]');
  await expect(celonisCard).toContainText('🎯 Junior / Entry Fit');

  // 1-Click Job Triage: clicking ⭐ Save increments the main navigation bar Saved counter and displays on #saved.
  await expect(page.locator('.saved-count')).toHaveText('0');
  await cabifyCard.getByRole('button', { name: '⭐ Save', exact: true }).click();
  await expect(page.locator('.saved-count')).toHaveText('1');
  await page.locator('.site-nav a[href="#saved"]').click();
  await expect(page.locator('[data-ats-job="greenhouse:cabify:101"]')).toBeVisible();
  await page.locator('.site-nav a[href="#opportunities"]').click();
  await cabifyCard.getByRole('button', { name: '⭐ Save', exact: true }).click();
  await expect(page.locator('.saved-count')).toHaveText('0');

  // Marking a card as Applied smoothly animates and hides it while Hide Applied & Dismissed is active.
  await cabifyCard.getByRole('button', { name: '✅ Applied', exact: true }).click();
  await expect(page.locator('[data-ats-job="greenhouse:cabify:101"]')).toHaveCount(0);
  await page.getByRole('button', { name: /View Applied/ }).click();
  await expect(page.locator('[data-ats-job="greenhouse:cabify:101"]')).toBeVisible();
  // Unmark Applied and return to normal feed.
  await page.locator('[data-ats-job="greenhouse:cabify:101"]').getByRole('button', { name: '✅ Applied', exact: true }).click();
  await page.getByRole('button', { name: /View Applied/ }).click();

  const invopopCard = page.locator('[data-ats-job="ashby:invopop:ash-1"]');
  await expect(invopopCard).toContainText('⚡ Posted 30h ago');
  await expect(invopopCard).toContainText('€45K – €58K');

  // Inspect board health diagnostics drawer showing partial upstream timeout.
  await page.getByRole('button', { name: /Board status/ }).click();
  await expect(page.getByText('Timed out after 8000ms')).toBeVisible();

  // Open Auto-Discovery panel and detect an embedded Personio board from a corporate career URL.
  await page.getByRole('button', { name: /➕ Discover \/ Add Companies/ }).click();
  await page.getByLabel('Career Page URL Auto-Sniffer').fill('https://www.paradigmadigital.com/empleo/');
  await page.getByRole('button', { name: 'Detect & Add ATS', exact: true }).click();
  await expect(page.getByText('Detected 1 public ATS board(s) with 8 open role(s).')).toBeVisible();
  await expect(page.locator('.ats-discovery-results')).toContainText('Paradigma Digital');

  // Switch to SAP, ERP & Enterprise CRM and hydrate SmartRecruiters description on demand.
  await page.getByRole('button', { name: /SAP, ERP & Enterprise CRM/ }).click();
  await expect(page.locator('[data-ats-job]')).toHaveCount(1);
  await celonisCard.getByRole('button', { name: 'Load full description', exact: true }).click();
  await expect(celonisCard).toContainText('SAP S/4HANA clean core development');
  // Newly hydrated keywords immediately highlight on the card.
  await expect(celonisCard.locator('.ats-match-chip', { hasText: 'OData' })).toBeVisible();
  await expect(celonisCard.locator('.ats-match-chip', { hasText: 'RAP' })).toBeVisible();

  // Switch to Software & Web Development and combine Spain + EU Remote + Fresh (< 48h).
  await page.getByRole('button', { name: /Software & Web Development/ }).click();
  await expect(page.locator('[data-ats-job]')).toHaveCount(3);
  await page.getByRole('button', { name: /Fresh \(< 48h\)/ }).click();
  await expect(page.locator('[data-ats-job]')).toHaveCount(2);
  await page.getByRole('button', { name: /Fresh \(< 48h\)/ }).click();

  // Quick-filter Junior / Entry narrows All Tech to Junior-flagged postings, and Recent (< 30d) is toggleable.
  await page.getByRole('button', { name: /All Tech \/ Software Roles/ }).click();
  await page.getByTestId('quick-filter-junior').click();
  await expect(page.locator('[data-ats-job]')).toHaveCount(1);
  await expect(page.locator('[data-ats-job="smartrecruiters:Celonis:sr-sap"]')).toBeVisible();
  await page.getByTestId('quick-filter-recent30d').click();
  await expect(page.locator('[data-ats-job]')).toHaveCount(1);

  // Switch to All Fetched Jobs to reset quick filters and reveal all 6 postings worldwide (including Fake US Remote and Sales).
  await page.getByRole('button', { name: /All Fetched Jobs/ }).click();
  await expect(page.locator('[data-ats-job]')).toHaveCount(6);

  // Custom Profile tag editing filters cleanly.
  await page.getByRole('button', { name: /Custom Profile/ }).click();
  await page.getByLabel('Add stack keyword').fill('Kubernetes');
  await page.getByRole('button', { name: 'Add tag', exact: true }).click();
  await expect(page.locator('.ats-custom-tags')).toContainText('Kubernetes');

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

  // Shortcut pill jumps straight into SAP, ERP & Enterprise CRM on Opportunities.
  await page.locator('.home-preset-shortcuts a', { hasText: 'SAP, ERP & Enterprise CRM' }).click();
  await expect(page.getByRole('button', { name: /SAP, ERP & Enterprise CRM/ })).toHaveAttribute('aria-pressed', 'true');
  await expect(page.locator('[data-ats-job="smartrecruiters:Celonis:sr-sap"]')).toBeVisible();
  await expect(page.getByRole('button', { name: /Python, Data & Analytics/ })).toBeVisible();
  await expect(page.getByRole('button', { name: /AI & Machine Learning/ })).toBeVisible();
  await expect(page.getByRole('button', { name: /Cloud, DevOps & Systems/ })).toBeVisible();
  await expect(page.getByRole('button', { name: /QA Automation & Cybersecurity/ })).toBeVisible();

  // Switch language to Spanish in place.
  await page.getByRole('button', { name: 'Español', exact: true }).click();
  await expect(page.getByRole('button', { name: 'Portales ATS en directo', exact: true })).toHaveAttribute('aria-pressed', 'true');
  await expect(page.getByText(/portales de empresa activos/)).toBeVisible();
  await expect(page.getByRole('button', { name: /🇪🇺 España \+ Remoto UE/ })).toBeVisible();
  await expect(page.getByTestId('quick-filter-recent30d')).toContainText('📅 Recientes (< 30d)');
  await expect(page.getByTestId('quick-filter-junior')).toContainText('🎯 Junior / Sin exp.');
  await expect(page.getByRole('button', { name: /Python, Datos y Analítica/ })).toBeVisible();
  await expect(page.getByRole('button', { name: /Desarrollo de Software y Web/ })).toBeVisible();

  // Verify narrow mobile viewport has zero horizontal overflow.
  await page.setViewportSize({ width: 320, height: 800 });
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= window.innerWidth)).toBe(true);
  expect(errors).toEqual([]);
});
