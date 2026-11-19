import { expect, test } from '@playwright/test';
import { parseAtsFeed, type AtsJob } from '../src/api/ats';
import {
  classifyGeoTier,
  detectSenioritySignal,
  evaluateAtsJob,
  formatFreshness,
  isSpainMadridOrRemote,
  matchesGeoScope,
  matchesKeyword,
  sortScoredJobs,
} from '../src/ats/scoring';

function makeJob(overrides: Partial<AtsJob> = {}): AtsJob {
  return {
    id: 'greenhouse:cabify:1',
    sourceId: '1',
    provider: 'greenhouse',
    providerLabel: 'Greenhouse',
    companySlug: 'cabify',
    companyName: 'Cabify',
    title: 'Software Engineer',
    location: 'Madrid, Spain',
    workplaceType: 'hybrid',
    publishedAt: '2026-11-17T08:00:00Z',
    hoursSincePublished: 4,
    description: 'Build services in Java 21, Spring Boot, PostgreSQL and Docker.',
    descriptionSnippet: 'Build services in Java 21, Spring Boot, PostgreSQL and Docker.',
    applyUrl: 'https://boards.greenhouse.io/cabify/jobs/1',
    department: 'Engineering',
    compensation: null,
    hasFullDescription: true,
    ...overrides,
  };
}

test('matchesKeyword respects token boundaries and avoids false substrings', () => {
  expect(matchesKeyword('Java 21 and Spring Boot', 'Java')).toBe(true);
  expect(matchesKeyword('Modern JavaScript and React', 'Java')).toBe(false);

  expect(matchesKeyword('SAP ABAP RAP & OData v4 development', 'RAP')).toBe(true);
  expect(matchesKeyword('Bootstrap CSS and graphic design wrapper', 'RAP')).toBe(false);

  expect(matchesKeyword('SAP CAP on BTP with Fiori', 'CAP')).toBe(true);
  expect(matchesKeyword('Strong capacity and leadership capabilities', 'CAP')).toBe(false);

  expect(matchesKeyword('Please reply ASAP to disappear', 'SAP')).toBe(false);
  expect(matchesKeyword('SAP S/4HANA clean core migration', 'SAP')).toBe(true);
  expect(matchesKeyword('SAP S4HANA clean core migration', 'S/4HANA')).toBe(true);
  expect(matchesKeyword('NodeJS + ReactJS + Postgres', 'Node.js')).toBe(true);
  expect(matchesKeyword('NodeJS + ReactJS + Postgres', 'React')).toBe(true);
  expect(matchesKeyword('NodeJS + ReactJS + Postgres', 'PostgreSQL')).toBe(true);
});

test('3-Tier Geographical Filter distinguishes Spain Only, EU Remote, and blocks Fake US/LATAM/UK Remote', () => {
  const madridJob = evaluateAtsJob(
    makeJob({ location: 'Madrid, Spain', workplaceType: 'onsite' }),
    'all-tech',
    [],
  );
  const pozueloJob = evaluateAtsJob(
    makeJob({ location: 'Pozuelo de Alarcón (Madrid)', workplaceType: 'hybrid' }),
    'all-tech',
    [],
  );
  const euRemoteJob = evaluateAtsJob(
    makeJob({ location: 'Remote - Europe (CET)', workplaceType: 'remote' }),
    'all-tech',
    [],
  );
  const fakeUsRemote = evaluateAtsJob(
    makeJob({ location: 'Remote (US Only)', workplaceType: 'remote', title: 'Software Engineer (Remote - US)' }),
    'all-tech',
    [],
  );
  const fakeUkRemote = evaluateAtsJob(
    makeJob({ location: 'Remote - UK only', workplaceType: 'remote' }),
    'all-tech',
    [],
  );
  const onsiteSf = evaluateAtsJob(
    makeJob({ location: 'San Francisco, CA', workplaceType: 'onsite' }),
    'all-tech',
    [],
  );

  expect(madridJob.isSpainOnly).toBe(true);
  expect(madridJob.isSpainOrEuRemote).toBe(true);
  expect(pozueloJob.isSpainOnly).toBe(true);

  expect(euRemoteJob.isSpainOnly).toBe(false);
  expect(euRemoteJob.isSpainOrEuRemote).toBe(true);
  expect(euRemoteJob.geoTier).toBe('eu-remote');

  expect(fakeUsRemote.isFakeRemoteBlocked).toBe(true);
  expect(fakeUsRemote.isSpainOrEuRemote).toBe(false);
  expect(matchesGeoScope(fakeUsRemote, 'spain-eu-remote')).toBe(false);
  expect(matchesGeoScope(fakeUsRemote, 'global')).toBe(true);

  expect(fakeUkRemote.isFakeRemoteBlocked).toBe(true);
  expect(fakeUkRemote.isSpainOrEuRemote).toBe(false);

  expect(onsiteSf.isSpainOrEuRemote).toBe(false);
  expect(isSpainMadridOrRemote(makeJob({ location: 'Remote - EMEA', workplaceType: null }))).toBe(true);
  expect(classifyGeoTier(makeJob({ location: 'Remote - LATAM', workplaceType: 'remote' })).isFakeRemoteBlocked).toBe(true);
});

test('boosts Junior/Entry roles, penalizes Staff/Principal roles, and ranks Spain Tier 1 above EU Remote Tier 2', () => {
  const juniorMadrid = evaluateAtsJob(
    makeJob({ id: 'jr', title: 'Junior Java Spring Boot Developer', location: 'Madrid, Spain', hoursSincePublished: 6 }),
    'fullstack',
    [],
  );
  const staffRole = evaluateAtsJob(
    makeJob({ id: 'staff', title: 'Staff Principal Java Engineer', location: 'Madrid, Spain', hoursSincePublished: 6 }),
    'fullstack',
    [],
  );

  expect(juniorMadrid.senioritySignal).toBe('junior');
  expect(staffRole.senioritySignal).toBe('senior-warning');
  expect(juniorMadrid.score).toBeGreaterThan(staffRole.score);
  expect(detectSenioritySignal({ title: 'Software Engineer Intern (Beca)', description: '' }).signal).toBe('junior');
  expect(detectSenioritySignal({ title: 'Engineering Manager', description: '10+ years required' }).signal).toBe('senior-warning');
});

test('ranks matched fresh roles above stale non-matches and formats Blip freshness badges', () => {
  const hotFullstack = evaluateAtsJob(
    makeJob({ id: 'a', title: 'Full Stack Java & Angular Developer', hoursSincePublished: 6 }),
    'fullstack',
    [],
  );
  const olderDescOnly = evaluateAtsJob(
    makeJob({ id: 'b', title: 'Platform Specialist', description: 'Uses Docker.', hoursSincePublished: 120 }),
    'fullstack',
    [],
  );
  const sapRole = evaluateAtsJob(
    makeJob({ id: 'c', title: 'SAP ABAP / RAP Consultant', description: 'S/4HANA OData v4 Fiori BTP', hoursSincePublished: 30 }),
    'sap',
    [],
  );

  expect(hotFullstack.isMatch).toBe(true);
  expect(hotFullstack.isFresh24h).toBe(true);
  expect(hotFullstack.matchedKeywords).toContain('Java');
  expect(hotFullstack.matchedKeywords).toContain('Angular');

  expect(sapRole.isMatch).toBe(true);
  expect(sapRole.isFresh24h).toBe(false);
  expect(sapRole.isFresh48h).toBe(true);
  expect(sapRole.matchedKeywords).toEqual(expect.arrayContaining(['SAP', 'ABAP', 'RAP', 'S/4HANA', 'OData', 'Fiori', 'BTP']));

  const ranked = sortScoredJobs([olderDescOnly, hotFullstack], 'relevance');
  expect(ranked.map((item) => item.job.id)).toEqual(['a', 'b']);

  expect(formatFreshness(0, null, 'en')).toEqual({ tier: 'hot', text: '🔥 Just posted' });
  expect(formatFreshness(6, null, 'en')).toEqual({ tier: 'hot', text: '🔥 Posted 6h ago' });
  expect(formatFreshness(36, null, 'en')).toEqual({ tier: 'fresh', text: '⚡ Posted 36h ago' });
  expect(formatFreshness(6, null, 'es')).toEqual({ tier: 'hot', text: '🔥 Hace 6h' });
});

test('parseAtsFeed accepts all 9 ATS providers and strips unsafe non-https applyUrls', () => {
  const parsed = parseAtsFeed({
    fetchedAt: '2026-11-17T12:00:00Z',
    cached: false,
    cacheAgeSeconds: 0,
    totalBoards: 2,
    succeededBoards: 2,
    failedBoards: 0,
    boards: [
      {
        companySlug: 'cabify',
        companyName: 'Cabify',
        provider: 'greenhouse',
        providerLabel: 'Greenhouse',
        ok: true,
        jobCount: 1,
        durationMs: 120,
        error: null,
      },
      {
        companySlug: 'paradigmadigital',
        companyName: 'Paradigma Digital',
        provider: 'personio',
        providerLabel: 'Personio',
        ok: true,
        jobCount: 1,
        durationMs: 95,
        error: null,
      },
    ],
    jobs: [
      makeJob(),
      makeJob({ id: 'personio:paradigmadigital:9', provider: 'personio', providerLabel: 'Personio' }),
      makeJob({ id: 'bad', applyUrl: 'javascript:alert(1)' }),
    ],
  });
  expect(parsed.jobs).toHaveLength(2);
  expect(parsed.jobs[0].id).toBe('greenhouse:cabify:1');
  expect(parsed.jobs[1].provider).toBe('personio');
  expect(() => parseAtsFeed(null)).toThrow();
  expect(() => parseAtsFeed({ jobs: [] })).toThrow();
});
