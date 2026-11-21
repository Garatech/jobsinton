export type AtsProviderId =
  | 'greenhouse'
  | 'lever'
  | 'smartrecruiters'
  | 'workable'
  | 'ashby'
  | 'personio'
  | 'recruitee'
  | 'teamtailor'
  | 'factorial';

export interface AtsJob {
  id: string;
  sourceId: string;
  provider: AtsProviderId;
  providerLabel: string;
  companySlug: string;
  companyName: string;
  title: string;
  location: string | null;
  workplaceType: 'remote' | 'hybrid' | 'onsite' | null;
  publishedAt: string | null;
  firstSeenAt?: string | null;
  hoursSincePublished: number | null;
  description: string;
  descriptionSnippet: string;
  applyUrl: string;
  department: string | null;
  compensation: string | null;
  hasFullDescription: boolean;
}

export interface BoardStatus {
  companySlug: string;
  companyName: string;
  provider: AtsProviderId;
  providerLabel: string;
  ok: boolean;
  jobCount: number;
  durationMs: number;
  error: string | null;
}

export interface AtsFeedResponse {
  fetchedAt: string;
  cached: boolean;
  cacheAgeSeconds: number;
  totalBoards: number;
  succeededBoards: number;
  failedBoards: number;
  boards: BoardStatus[];
  jobs: AtsJob[];
}

export interface SmartRecruitersDetail {
  id: string;
  description: string;
  descriptionSnippet: string;
  applyUrl: string;
  hasFullDescription: boolean;
}

export interface DiscoveredBoardResult {
  slug: string;
  name: string;
  provider: AtsProviderId;
  providerLabel: string;
  apiEndpoint: string | null;
  jobCount: number;
  newlyRegistered: boolean;
}

export interface AtsDiscoveryResponse {
  discovered: DiscoveredBoardResult[];
  totalAdded: number;
  totalJobsFound: number;
  message: string;
}

const VALID_PROVIDERS = new Set<AtsProviderId>([
  'greenhouse',
  'lever',
  'smartrecruiters',
  'workable',
  'ashby',
  'personio',
  'recruitee',
  'teamtailor',
  'factorial',
]);
const VALID_WORKPLACE = new Set(['remote', 'hybrid', 'onsite']);
const CLIENT_CACHE_TTL_MS = 10 * 60 * 1000;
const FEED_TIMEOUT_MS = 25_000;
const DETAIL_TIMEOUT_MS = 10_000;

let memoryCache: { feed: AtsFeedResponse; storedAt: number } | null = null;
let inflightFeed: Promise<AtsFeedResponse> | null = null;

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === 'object' && value !== null && !Array.isArray(value);
}

function optionalString(value: unknown): string | null {
  return typeof value === 'string' && value.trim().length > 0 ? value : null;
}

function validHttpsUrl(value: unknown): string | null {
  if (typeof value !== 'string') return null;
  try {
    const parsed = new URL(value);
    if (parsed.protocol !== 'https:' || !parsed.hostname) return null;
    return parsed.toString();
  } catch {
    return null;
  }
}

function parseProvider(value: unknown): AtsProviderId {
  if (typeof value === 'string' && VALID_PROVIDERS.has(value as AtsProviderId)) {
    return value as AtsProviderId;
  }
  throw new Error('Invalid ATS provider identifier.');
}

function parseBoard(raw: unknown): BoardStatus {
  if (!isRecord(raw)
    || typeof raw.companySlug !== 'string'
    || typeof raw.companyName !== 'string'
    || typeof raw.providerLabel !== 'string'
    || typeof raw.ok !== 'boolean'
    || typeof raw.jobCount !== 'number'
    || typeof raw.durationMs !== 'number') {
    throw new Error('Malformed board status entry.');
  }
  return {
    companySlug: raw.companySlug,
    companyName: raw.companyName,
    provider: parseProvider(raw.provider),
    providerLabel: raw.providerLabel,
    ok: raw.ok,
    jobCount: raw.jobCount,
    durationMs: raw.durationMs,
    error: optionalString(raw.error),
  };
}

function parseJob(raw: unknown): AtsJob | null {
  if (!isRecord(raw)
    || typeof raw.id !== 'string'
    || typeof raw.sourceId !== 'string'
    || typeof raw.companySlug !== 'string'
    || typeof raw.companyName !== 'string'
    || typeof raw.providerLabel !== 'string'
    || typeof raw.title !== 'string' || !raw.title.trim()) {
    return null;
  }
  const applyUrl = validHttpsUrl(raw.applyUrl);
  if (!applyUrl) return null;
  if (typeof raw.provider !== 'string' || !VALID_PROVIDERS.has(raw.provider as AtsProviderId)) return null;
  const workplaceRaw = optionalString(raw.workplaceType);
  const workplaceType = workplaceRaw && VALID_WORKPLACE.has(workplaceRaw)
    ? (workplaceRaw as 'remote' | 'hybrid' | 'onsite')
    : null;
  const publishedAt = optionalString(raw.publishedAt);
  const firstSeenAt = optionalString(raw.firstSeenAt) ?? publishedAt;
  return {
    id: raw.id,
    sourceId: raw.sourceId,
    provider: raw.provider as AtsProviderId,
    providerLabel: raw.providerLabel,
    companySlug: raw.companySlug,
    companyName: raw.companyName,
    title: raw.title.trim(),
    location: optionalString(raw.location),
    workplaceType,
    publishedAt,
    firstSeenAt,
    hoursSincePublished: typeof raw.hoursSincePublished === 'number' && Number.isFinite(raw.hoursSincePublished)
      ? Math.max(0, Math.floor(raw.hoursSincePublished))
      : null,
    description: typeof raw.description === 'string' ? raw.description : '',
    descriptionSnippet: typeof raw.descriptionSnippet === 'string' ? raw.descriptionSnippet : '',
    applyUrl,
    department: optionalString(raw.department),
    compensation: optionalString(raw.compensation),
    hasFullDescription: Boolean(raw.hasFullDescription),
  };
}

export function parseAtsFeed(payload: unknown): AtsFeedResponse {
  if (!isRecord(payload)
    || typeof payload.fetchedAt !== 'string'
    || typeof payload.cached !== 'boolean'
    || typeof payload.cacheAgeSeconds !== 'number'
    || typeof payload.totalBoards !== 'number'
    || typeof payload.succeededBoards !== 'number'
    || typeof payload.failedBoards !== 'number'
    || !Array.isArray(payload.boards)
    || !Array.isArray(payload.jobs)) {
    throw new Error('The live ATS feed response could not be verified.');
  }
  const boards = payload.boards.map(parseBoard);
  const jobs = payload.jobs.map(parseJob).filter((job): job is AtsJob => job !== null);
  return {
    fetchedAt: payload.fetchedAt,
    cached: payload.cached,
    cacheAgeSeconds: payload.cacheAgeSeconds,
    totalBoards: payload.totalBoards,
    succeededBoards: payload.succeededBoards,
    failedBoards: payload.failedBoards,
    boards,
    jobs,
  };
}

export function parseSmartRecruitersDetail(payload: unknown): SmartRecruitersDetail {
  if (!isRecord(payload) || typeof payload.id !== 'string' || typeof payload.description !== 'string') {
    throw new Error('Could not verify SmartRecruiters posting details.');
  }
  const applyUrl = validHttpsUrl(payload.applyUrl);
  if (!applyUrl) {
    throw new Error('Invalid SmartRecruiters application URL.');
  }
  return {
    id: payload.id,
    description: payload.description,
    descriptionSnippet: typeof payload.descriptionSnippet === 'string' ? payload.descriptionSnippet : '',
    applyUrl,
    hasFullDescription: Boolean(payload.hasFullDescription),
  };
}

export function getCachedAtsFeed(): AtsFeedResponse | null {
  if (!memoryCache) return null;
  if (Date.now() - memoryCache.storedAt > CLIENT_CACHE_TTL_MS) {
    return null;
  }
  return memoryCache.feed;
}

export function clearAtsClientCache(): void {
  memoryCache = null;
}

export function patchCachedAtsJob(detail: SmartRecruitersDetail): AtsFeedResponse | null {
  if (!memoryCache) return null;
  const updatedJobs = memoryCache.feed.jobs.map((job) => (
    job.id === detail.id
      ? {
          ...job,
          description: detail.description,
          descriptionSnippet: detail.descriptionSnippet || job.descriptionSnippet,
          applyUrl: detail.applyUrl || job.applyUrl,
          hasFullDescription: detail.hasFullDescription,
        }
      : job
  ));
  memoryCache = {
    ...memoryCache,
    feed: { ...memoryCache.feed, jobs: updatedJobs },
  };
  return memoryCache.feed;
}

export async function fetchAtsFeed(options: { refresh?: boolean; signal?: AbortSignal } = {}): Promise<AtsFeedResponse> {
  const { refresh = false, signal } = options;
  if (!refresh) {
    const cached = getCachedAtsFeed();
    if (cached) return cached;
  }

  let sharedRun = !refresh ? inflightFeed : null;
  if (!sharedRun) {
    const run = (async () => {
      const controller = new AbortController();
      const timer = window.setTimeout(() => controller.abort(), FEED_TIMEOUT_MS);
      try {
        const url = refresh ? '/api/ats/jobs?refresh=true' : '/api/ats/jobs';
        const response = await fetch(url, {
          headers: { Accept: 'application/json' },
          cache: 'no-store',
          signal: controller.signal,
        });
        if (!response.ok) {
          throw new Error('The live ATS service returned an error. Check that the Java backend is running.');
        }
        const data: unknown = await response.json();
        const parsed = parseAtsFeed(data);
        memoryCache = { feed: parsed, storedAt: Date.now() };
        return parsed;
      } catch (error) {
        if (controller.signal.aborted) {
          throw new Error('Fetching live ATS boards took too long. Try refreshing again.');
        }
        if (error instanceof Error) throw error;
        throw new Error('Could not reach the local backend to load live ATS jobs.');
      } finally {
        window.clearTimeout(timer);
      }
    })();

    sharedRun = run;
    if (!refresh) {
      inflightFeed = run;
      run.finally(() => {
        if (inflightFeed === run) inflightFeed = null;
      });
    }
  }

  if (!signal) return sharedRun;
  if (signal.aborted) {
    return Promise.reject(new DOMException('The operation was aborted.', 'AbortError'));
  }

  return new Promise<AtsFeedResponse>((resolve, reject) => {
    const onAbort = () => reject(new DOMException('The operation was aborted.', 'AbortError'));
    signal.addEventListener('abort', onAbort, { once: true });
    sharedRun.then(
      (value) => {
        signal.removeEventListener('abort', onAbort);
        resolve(value);
      },
      (err) => {
        signal.removeEventListener('abort', onAbort);
        reject(err);
      },
    );
  });
}

export async function fetchSmartRecruitersDetail(
  companySlug: string,
  postingId: string,
  signal?: AbortSignal,
): Promise<SmartRecruitersDetail> {
  const controller = new AbortController();
  const onExternalAbort = () => controller.abort();
  if (signal) {
    if (signal.aborted) controller.abort();
    else signal.addEventListener('abort', onExternalAbort, { once: true });
  }
  const timer = window.setTimeout(() => controller.abort(), DETAIL_TIMEOUT_MS);
  try {
    const url = `/api/ats/smartrecruiters/${encodeURIComponent(companySlug)}/${encodeURIComponent(postingId)}`;
    const response = await fetch(url, {
      headers: { Accept: 'application/json' },
      cache: 'no-store',
      signal: controller.signal,
    });
    if (!response.ok) {
      throw new Error('Could not load the full job description from SmartRecruiters.');
    }
    const data: unknown = await response.json();
    const detail = parseSmartRecruitersDetail(data);
    patchCachedAtsJob(detail);
    return detail;
  } finally {
    window.clearTimeout(timer);
    signal?.removeEventListener('abort', onExternalAbort);
  }
}

export function parseAtsDiscoveryResponse(payload: unknown): AtsDiscoveryResponse {
  if (!isRecord(payload)
    || !Array.isArray(payload.discovered)
    || typeof payload.totalAdded !== 'number'
    || typeof payload.totalJobsFound !== 'number') {
    throw new Error('Could not verify ATS discovery response.');
  }
  const discovered: DiscoveredBoardResult[] = payload.discovered.map((item) => {
    if (!isRecord(item)
      || typeof item.slug !== 'string'
      || typeof item.name !== 'string'
      || typeof item.providerLabel !== 'string'
      || typeof item.jobCount !== 'number') {
      throw new Error('Malformed discovered ATS board entry.');
    }
    return {
      slug: item.slug,
      name: item.name,
      provider: parseProvider(item.provider),
      providerLabel: item.providerLabel,
      apiEndpoint: optionalString(item.apiEndpoint),
      jobCount: item.jobCount,
      newlyRegistered: Boolean(item.newlyRegistered),
    };
  });
  return {
    discovered,
    totalAdded: payload.totalAdded,
    totalJobsFound: payload.totalJobsFound,
    message: typeof payload.message === 'string' ? payload.message : '',
  };
}

export async function discoverAtsFromUrl(
  url: string,
  companyName?: string,
): Promise<AtsDiscoveryResponse> {
  const response = await fetch('/api/ats/discover/url', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', Accept: 'application/json' },
    body: JSON.stringify({ url: url.trim(), companyName: companyName?.trim() || null }),
  });
  const data: unknown = await response.json().catch(() => null);
  if (!response.ok) {
    if (isRecord(data) && typeof data.message === 'string') {
      throw new Error(data.message);
    }
    throw new Error('Could not inspect that career page URL.');
  }
  clearAtsClientCache();
  return parseAtsDiscoveryResponse(data);
}

export async function probeAtsCompanies(candidates: string[]): Promise<AtsDiscoveryResponse> {
  const response = await fetch('/api/ats/discover/probe', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', Accept: 'application/json' },
    body: JSON.stringify({ candidates }),
  });
  const data: unknown = await response.json().catch(() => null);
  if (!response.ok) {
    if (isRecord(data) && typeof data.message === 'string') {
      throw new Error(data.message);
    }
    throw new Error('Could not probe company names across ATS platforms.');
  }
  clearAtsClientCache();
  return parseAtsDiscoveryResponse(data);
}
