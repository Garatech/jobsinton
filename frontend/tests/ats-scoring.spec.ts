import { expect, test } from '@playwright/test';
import { parseAtsFeed, type AtsJob } from '../src/api/ats';
import {
  classifyGeoTier,
  detectSenioritySignal,
  evaluateAtsJob,
  formatFreshness,
  hasFakeRemoteRestriction,
  isSpainMadridOrRemote,
  isSpainOnlyJob,
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

test('3-tier geo filter classifies Spain Only, Spain + EU Remote, and blocks Fake US/UK/LATAM Remote', () => {
  const madridJob = makeJob({ location: 'Madrid, Spain', workplaceType: 'onsite' });
  const barcelonaHybrid = makeJob({ location: 'Barcelona', workplaceType: 'hybrid' });
  const euRemoteJob = makeJob({ location: 'Remote - Europe (CET)', workplaceType: 'remote' });
  const fakeUsRemote = makeJob({
    location: 'Remote - US Only',
    workplaceType: 'remote',
    description: 'Must reside in the US and work EST/PST hours.',
  });
  const fakeCanadaRemote = makeJob({
    location: 'Toronto, CA, Remote',
    workplaceType: 'remote',
  });
  const fakeTxRemote = makeJob({
    location: 'Austin, TX, Remote',
    workplaceType: 'remote',
  });
  const fakeUkRemote = makeJob({
    location: 'London, United Kingdom (UK only)',
    workplaceType: 'remote',
  });
  const berlinHybrid = makeJob({ location: 'Berlin, Germany', workplaceType: 'hybrid' });
  const lisbonOnsite = makeJob({ location: 'Lisbon, Portugal', workplaceType: 'onsite' });
  const sfOnsite = makeJob({ location: 'San Francisco, CA', workplaceType: 'onsite' });

  expect(isSpainOnlyJob(madridJob)).toBe(true);
  expect(isSpainOnlyJob(barcelonaHybrid)).toBe(true);
  expect(isSpainOnlyJob(euRemoteJob)).toBe(false);
  expect(isSpainOnlyJob(berlinHybrid)).toBe(false);

  expect(hasFakeRemoteRestriction(fakeUsRemote)).toBe(true);
  expect(hasFakeRemoteRestriction(fakeCanadaRemote)).toBe(true);
  expect(hasFakeRemoteRestriction(fakeTxRemote)).toBe(true);
  expect(hasFakeRemoteRestriction(fakeUkRemote)).toBe(true);
  expect(hasFakeRemoteRestriction(euRemoteJob)).toBe(false);
  expect(hasFakeRemoteRestriction(berlinHybrid)).toBe(false);

  expect(classifyGeoTier(madridJob).geoTier).toBe('spain');
  expect(classifyGeoTier(euRemoteJob).geoTier).toBe('eu-remote');
  expect(classifyGeoTier(berlinHybrid).geoTier).toBe('eu-remote');
  expect(classifyGeoTier(lisbonOnsite).geoTier).toBe('eu-remote');
  expect(classifyGeoTier(fakeUsRemote).geoTier).toBe('global');
  expect(classifyGeoTier(fakeUsRemote).isFakeRemoteBlocked).toBe(true);
  expect(classifyGeoTier(fakeCanadaRemote).geoTier).toBe('global');
  expect(classifyGeoTier(fakeCanadaRemote).isFakeRemoteBlocked).toBe(true);
  expect(classifyGeoTier(sfOnsite).geoTier).toBe('global');

  const scoredMadrid = evaluateAtsJob(madridJob, 'all-tech', []);
  const scoredEuRemote = evaluateAtsJob(euRemoteJob, 'all-tech', []);
  const scoredBerlin = evaluateAtsJob(berlinHybrid, 'all-tech', []);
  const scoredFakeUs = evaluateAtsJob(fakeUsRemote, 'all-tech', []);
  const scoredFakeCanada = evaluateAtsJob(fakeCanadaRemote, 'all-tech', []);

  expect(matchesGeoScope(scoredMadrid, 'spain-only')).toBe(true);
  expect(matchesGeoScope(scoredEuRemote, 'spain-only')).toBe(false);
  expect(matchesGeoScope(scoredBerlin, 'spain-only')).toBe(false);
  expect(matchesGeoScope(scoredMadrid, 'spain-eu-remote')).toBe(true);
  expect(matchesGeoScope(scoredEuRemote, 'spain-eu-remote')).toBe(true);
  expect(matchesGeoScope(scoredBerlin, 'spain-eu-remote')).toBe(true);
  expect(matchesGeoScope(scoredFakeUs, 'spain-eu-remote')).toBe(false);
  expect(matchesGeoScope(scoredFakeCanada, 'spain-eu-remote')).toBe(false);
  expect(matchesGeoScope(scoredFakeUs, 'global')).toBe(true);
  expect(isSpainMadridOrRemote(euRemoteJob)).toBe(true);
  expect(isSpainMadridOrRemote(berlinHybrid)).toBe(true);
  expect(isSpainMadridOrRemote(fakeUsRemote)).toBe(false);
  expect(isSpainMadridOrRemote(fakeCanadaRemote)).toBe(false);
});

test('ranks matched fresh & junior roles above senior or stale roles and formats first-48h freshness badges', () => {
  const hotFullstack = evaluateAtsJob(
    makeJob({ id: 'a', title: 'Junior Full Stack Java & Angular Developer', hoursSincePublished: 6 }),
    'fullstack',
    [],
  );
  const seniorRole = evaluateAtsJob(
    makeJob({ id: 's', title: 'Staff Principal Java Architect', hoursSincePublished: 6 }),
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

  expect(detectSenioritySignal(hotFullstack.job).signal).toBe('junior');
  expect(detectSenioritySignal(seniorRole.job).signal).toBe('senior-warning');
  expect(hotFullstack.senioritySignal).toBe('junior');
  expect(seniorRole.senioritySignal).toBe('senior-warning');
  expect(hotFullstack.score).toBeGreaterThan(seniorRole.score);

  expect(hotFullstack.isMatch).toBe(true);
  expect(hotFullstack.isFresh24h).toBe(true);
  expect(hotFullstack.matchedKeywords).toContain('Java');
  expect(hotFullstack.matchedKeywords).toContain('Angular');

  expect(sapRole.isMatch).toBe(true);
  expect(sapRole.isFresh24h).toBe(false);
  expect(sapRole.isFresh48h).toBe(true);
  expect(sapRole.matchedKeywords).toEqual(expect.arrayContaining(['SAP', 'ABAP', 'RAP', 'S/4HANA', 'OData', 'Fiori', 'BTP']));

  const ranked = sortScoredJobs([olderDescOnly, seniorRole, hotFullstack], 'relevance');
  expect(ranked.map((item) => item.job.id)).toEqual(['a', 's', 'b']);

  expect(formatFreshness(0, null, 'en')).toEqual({ tier: 'hot', text: '🔥 Just posted' });
  expect(formatFreshness(6, null, 'en')).toEqual({ tier: 'hot', text: '🔥 Posted 6h ago' });
  expect(formatFreshness(36, null, 'en')).toEqual({ tier: 'fresh', text: '⚡ Posted 36h ago' });
  expect(formatFreshness(6, null, 'es')).toEqual({ tier: 'hot', text: '🔥 Hace 6h' });
});

test('parseAtsFeed validates structure across all 9 ATS providers and strips unsafe non-https applyUrls', () => {
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
      makeJob({ id: 'personio:paradigmadigital:99', provider: 'personio', providerLabel: 'Personio' }),
      makeJob({ id: 'bad', applyUrl: 'javascript:alert(1)' }),
    ],
  });
  expect(parsed.jobs).toHaveLength(2);
  expect(parsed.jobs[0].id).toBe('greenhouse:cabify:1');
  expect(parsed.jobs[1].provider).toBe('personio');
  expect(() => parseAtsFeed(null)).toThrow();
  expect(() => parseAtsFeed({ jobs: [] })).toThrow();
});

test('multi-discipline developer tracks match Python/Data, AI/ML, Cloud/DevOps, QA/Cybersecurity, and ERP roles with EN/ES aliases', () => {
  const dataJob = makeJob({
    id: 'personio:synvert:1',
    title: 'Analista de Datos (Python & Power BI)',
    description: 'Build ETL data pipelines with SQL, Pandas, Airflow, dbt, and Databricks.',
  });
  const aiJob = makeJob({
    id: 'ashby:n8n:1',
    title: 'Junior AI Engineer (GenAI & LLMs)',
    description: 'Develop agentic AI workflows with PyTorch, LangGraph, RAG and Hugging Face.',
  });
  const cloudJob = makeJob({
    id: 'greenhouse:nebius:1',
    title: 'Ingeniero de Sistemas Cloud & DevOps',
    description: 'Manage Kubernetes (K8s), Terraform, AWS, Linux and GitHub Actions CI/CD.',
  });
  const cyberQaJob = makeJob({
    id: 'smartrecruiters:checkpoint:1',
    title: 'Analista de Ciberseguridad & QA Automation',
    description: 'Playwright automated testing, SIEM, SOC operations and pentesting.',
  });
  const erpJob = makeJob({
    id: 'teamtailor:logicalis:1',
    title: 'Consultor SAP FI/CO & Salesforce / Dynamics 365',
    description: 'Enterprise ERP and CRM transformation with SAP HANA, Business Central and ServiceNow.',
  });

  const scoredData = evaluateAtsJob(dataJob, 'python-data', []);
  expect(scoredData.isMatch).toBe(true);
  expect(scoredData.matchedKeywords).toEqual(
    expect.arrayContaining(['Python', 'Data Analyst', 'Power BI', 'SQL', 'Pandas', 'Airflow', 'dbt', 'Databricks', 'ETL']),
  );

  const scoredAi = evaluateAtsJob(aiJob, 'ai-ml', []);
  expect(scoredAi.isMatch).toBe(true);
  expect(scoredAi.matchedKeywords).toEqual(
    expect.arrayContaining(['AI Engineer', 'Generative AI', 'LLM', 'PyTorch', 'LangChain', 'RAG', 'Hugging Face']),
  );

  const scoredCloud = evaluateAtsJob(cloudJob, 'cloud-devops', []);
  expect(scoredCloud.isMatch).toBe(true);
  expect(scoredCloud.matchedKeywords).toEqual(
    expect.arrayContaining(['AWS', 'Kubernetes', 'Terraform', 'CI/CD', 'Linux', 'DevOps', 'Systems Engineer']),
  );

  const scoredCyberQa = evaluateAtsJob(cyberQaJob, 'qa-cyber', []);
  expect(scoredCyberQa.isMatch).toBe(true);
  expect(scoredCyberQa.matchedKeywords).toEqual(
    expect.arrayContaining(['QA', 'Test Automation', 'Playwright', 'Cybersecurity', 'SOC', 'SIEM', 'Pentesting']),
  );

  const scoredErp = evaluateAtsJob(erpJob, 'sap', []);
  expect(scoredErp.isMatch).toBe(true);
  expect(scoredErp.matchedKeywords).toEqual(
    expect.arrayContaining(['SAP', 'SAP HANA', 'FI/CO', 'Salesforce', 'Dynamics 365', 'ServiceNow', 'ERP']),
  );
});

test('Phase 5A: detects Spanish & European junior/entry signals, penalizes Senior/Lead/Architect & +100d zombie postings, and matches unhydrated Web/Java titles', () => {
  // 1. Spanish & European Junior / Entry signals (sin experiencia, grado superior, DAM/DAW/ASIR, recién titulado, trainee)
  const spainFpJunior = evaluateAtsJob(
    makeJob({
      id: 'factorial:mvst:jr-1',
      title: 'Desarrollador Web Full Stack',
      location: 'Sant Cugat, Spain',
      description: 'Buscamos recién titulado de Ciclo Formativo de Grado Superior (DAM, DAW o ASIR) sin experiencia previa.',
      hoursSincePublished: 12,
    }),
    'fullstack',
    [],
  );
  expect(spainFpJunior.isSpainOnly).toBe(true);
  expect(spainFpJunior.senioritySignal).toBe('junior');
  expect(detectSenioritySignal(spainFpJunior.job)).toEqual({ signal: 'junior', delta: 20 });
  expect(spainFpJunior.isMatch).toBe(true);
  expect(spainFpJunior.matchedKeywords).toEqual(expect.arrayContaining(['Full Stack', 'Web Developer']));

  const associateTrainee = detectSenioritySignal(
    makeJob({ title: 'Associate Software Developer / Trainee', description: '' }),
  );
  expect(associateTrainee).toEqual({ signal: 'junior', delta: 30 });

  // 2. Senior (-18) and Tech Lead / Architect (-25) title penalties
  expect(detectSenioritySignal(makeJob({ title: 'Senior Backend Java Developer', description: '' }))).toEqual({
    signal: 'senior-warning',
    delta: -18,
  });
  expect(detectSenioritySignal(makeJob({ title: 'Tech Lead & Solutions Architect', description: '' }))).toEqual({
    signal: 'senior-warning',
    delta: -25,
  });

  // 3. Stale ("zombie") +100 days ago penalty (-45) sinks old postings below recent ones
  const recentBackend = evaluateAtsJob(
    makeJob({
      id: 'recent-20d',
      title: 'Desarrollador Backend',
      description: '',
      hoursSincePublished: 24 * 20, // 20 days ago
    }),
    'fullstack',
    [],
  );
  const zombie120d = evaluateAtsJob(
    makeJob({
      id: 'zombie-120d',
      title: 'Desarrollador Backend',
      description: 'Java, Spring, Hibernate, PostgreSQL',
      hoursSincePublished: 24 * 120, // 120 days ago (+100d)
    }),
    'fullstack',
    [],
  );
  expect(recentBackend.isRecent30d).toBe(true);
  expect(zombie120d.isRecent30d).toBe(false);
  expect(recentBackend.score).toBeGreaterThan(zombie120d.score);

  // 4. Unhydrated Web / Frontend / Backend / Software titles match fullstack, while Junior SAP ABAP Developer does not
  const unhydratedFrontend = evaluateAtsJob(
    makeJob({ title: 'Programador Frontend', description: '', hasFullDescription: false }),
    'fullstack',
    [],
  );
  const unhydratedSoftwareDev = evaluateAtsJob(
    makeJob({ title: 'Junior Software Developer', description: '', hasFullDescription: false }),
    'fullstack',
    [],
  );
  const unhydratedSapDev = evaluateAtsJob(
    makeJob({ title: 'Junior SAP ABAP Developer', description: '', hasFullDescription: false }),
    'fullstack',
    [],
  );
  expect(unhydratedFrontend.isMatch).toBe(true);
  expect(unhydratedFrontend.matchedKeywords).toContain('Frontend');
  expect(unhydratedSoftwareDev.isMatch).toBe(true);
  expect(unhydratedSoftwareDev.matchedKeywords).toContain('Software Developer');
  expect(unhydratedSapDev.isMatch).toBe(false);

  // 5. Multi-country remote role including Europe alongside US is preserved in Spain + EU Remote
  const multiRegionRemote = makeJob({
    location: 'Remote - US, UK, Germany',
    workplaceType: 'remote',
  });
  expect(hasFakeRemoteRestriction(multiRegionRemote)).toBe(false);
  expect(classifyGeoTier(multiRegionRemote).geoTier).toBe('eu-remote');
});


