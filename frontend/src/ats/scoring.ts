import type { AtsJob } from '../api/ats';
import {
  ALL_TECH_HIGHLIGHT_KEYWORDS,
  FULLSTACK_KEYWORDS,
  SAP_KEYWORDS,
  type StackPresetId,
} from './presets';

export type GeoScope = 'spain-only' | 'spain-eu-remote' | 'global';
export type GeoTier = 'spain' | 'eu-remote' | 'global';
export type SenioritySignal = 'junior' | 'senior-warning' | 'neutral';

export interface ScoredAtsJob {
  job: AtsJob;
  matchedKeywords: string[];
  titleMatches: string[];
  score: number;
  isMatch: boolean;
  isTechRole: boolean;
  isFresh24h: boolean;
  isFresh48h: boolean;
  isSpainOnly: boolean;
  isSpainOrEuRemote: boolean;
  isSpainOrRemote: boolean;
  isFakeRemoteBlocked: boolean;
  geoTier: GeoTier;
  senioritySignal: SenioritySignal;
}

export type FreshnessTier = 'hot' | 'fresh' | 'recent' | 'normal';

export interface FreshnessBadge {
  tier: FreshnessTier;
  text: string;
}

const KEYWORD_ALIASES: Readonly<Record<string, readonly string[]>> = {
  java: ['java'],
  'spring boot': ['spring boot', 'springboot'],
  angular: ['angular'],
  'node.js': ['node.js', 'nodejs', 'node js', 'node'],
  react: ['react', 'react.js', 'reactjs'],
  typescript: ['typescript'],
  javascript: ['javascript'],
  postgresql: ['postgresql', 'postgres'],
  docker: ['docker'],
  sap: ['sap'],
  abap: ['abap', 'abap oo', 'abap cloud'],
  's/4hana': ['s/4hana', 's/4 hana', 's4hana', 's4/hana', 'sap hana'],
  rap: ['rap', 'abap rap'],
  cap: ['cap', 'sap cap', 'cloud application programming'],
  odata: ['odata', 'odata v4', 'odata v2'],
  fiori: ['fiori', 'sapui5', 'ui5'],
  btp: ['btp', 'sap btp'],
  'ci/cd': ['ci/cd', 'cicd', 'github actions', 'jenkins', 'gitlab ci'],
};

const TECH_TITLE_INDICATORS = [
  'engineer', 'developer', 'software', 'backend', 'back-end', 'frontend', 'front-end',
  'full stack', 'full-stack', 'fullstack', 'devops', 'sre', 'platform', 'architect',
  'data', 'cloud', 'sap', 'abap', 'fiori', 'qa', 'sdet', 'automation', 'security',
  'mobile', 'ios', 'android', 'web', 'systems', 'infrastructure', 'ml', 'ai',
  'desarrollador', 'desarrolladora', 'programador', 'programadora', 'ingeniero',
  'ingeniera', 'consultor sap', 'consultora sap', 'consultor', 'arquitecto', 'tech lead',
];

const SPAIN_ONLY_PATTERNS = [
  /\bmadrid\b/i,
  /\bbarcelona\b/i,
  /\bvalencia\b/i,
  /\bm[aá]laga\b/i,
  /\bsevilla\b/i,
  /\bbilbao\b/i,
  /\bzaragoza\b/i,
  /\bpozuelo\b/i,
  /\blas rozas\b/i,
  /\balcobendas\b/i,
  /\btres cantos\b/i,
  /\bgetafe\b/i,
  /\bboadilla\b/i,
  /\bspain\b/i,
  /\bespa[nñ]a\b/i,
  /\biberia\b/i,
  /\bgalicia\b/i,
  /\basturias\b/i,
  /\balicante\b/i,
  /\bvalladolid\b/i,
  /\bsan sebasti[aá]n\b/i,
  /\bcoru[nñ]a\b/i,
  /\bvigo\b/i,
  /\bgranada\b/i,
  /\boviedo\b/i,
  /\bmurcia\b/i,
  /\bcartagena\b/i,
  /\bcastell[oó]n\b/i,
];

const FAKE_REMOTE_BLOCK_PATTERNS = [
  /\bu\.?s\.?\s+only\b/i,
  /\bunited states\b/i,
  /\bremote\s*[-–(/]+\s*u\.?s\.?\b/i,
  /\bnorth america\b/i,
  /\bcanada(?:\s+only)?\b/i,
  /\blatam\b/i,
  /\blatin america\b/i,
  /\bbrazil\b/i,
  /\bmexico\b/i,
  /\bapac\b/i,
  /\bindia\b/i,
  /\bu\.?k\.?\s+only\b/i,
  /\bunited kingdom\s+only\b/i,
  /\baustralia\b/i,
  /\b(?:pst|est|cst)\b/i,
  /\bmust reside in the u\.?s\.?\b/i,
  /\bauthorized to work in the u\.?s\.?\b/i,
];

const EU_REMOTE_PATTERNS = [
  /\beurope\b/i,
  /\beuropean union\b/i,
  /\beu\b/i,
  /\bemea\b/i,
  /\bcet\b/i,
  /\bcest\b/i,
  /\butc\s*\+\s*1\b/i,
  /\banywhere\b/i,
  /\bworldwide\b/i,
  /\bdistributed\b/i,
  /\bremote\b/i,
  /\bremoto\b/i,
  /\bteletrabajo\b/i,
];

const JUNIOR_FRIENDLY_PATTERNS = [
  /\bjunior\b/i,
  /\bjr\.?\b/i,
  /\bintern(?:ship)?\b/i,
  /\bbeca(?:rio|ria)?\b/i,
  /\bpr[aá]cticas\b/i,
  /\bearly career\b/i,
  /\bgraduate\b/i,
  /\bentry[\s-]+level\b/i,
  /\bengineer\s+i\b/i,
  /\bdeveloper\s+i\b/i,
  /\b[012]\s*[-–to]+\s*[23]\s*(?:years|a[nñ]os)\b/i,
  /\b1\+?\s*(?:year|a[nñ]o)\b/i,
  /\b2\+?\s*(?:years|a[nñ]os)\b/i,
];

const SENIOR_STAFF_TITLE_PATTERNS = [
  /\bstaff\b/i,
  /\bprincipal\b/i,
  /\bdirector\b/i,
  /\bhead of\b/i,
  /\bengineering manager\b/i,
  /\bvp of\b/i,
];

const SENIOR_YEARS_PATTERNS = [
  /\b(?:[89]|1[0-5])\+?\s*(?:years|a[nñ]os)\b/i,
];

const regexCache = new Map<string, RegExp>();

function escapeRegex(value: string): string {
  return value.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
}

function boundaryRegex(term: string): RegExp {
  const key = term.toLowerCase();
  const cached = regexCache.get(key);
  if (cached) return cached;
  // Use custom token boundaries so "Java" never matches "JavaScript", "RAP" never matches "bootstrap",
  // and symbols like C++, C#, S/4HANA, and Node.js match cleanly.
  const pattern = new RegExp(`(?<![A-Za-z0-9_+#.])${escapeRegex(term)}(?![A-Za-z0-9_+#])`, 'i');
  regexCache.set(key, pattern);
  return pattern;
}

export function matchesKeyword(haystack: string, keyword: string): boolean {
  if (!haystack || !keyword.trim()) return false;
  const trimmed = keyword.trim();
  const aliases = KEYWORD_ALIASES[trimmed.toLowerCase()] ?? [trimmed];
  return aliases.some((alias) => boundaryRegex(alias).test(haystack));
}

export function isSpainOnlyJob(job: Pick<AtsJob, 'location' | 'title' | 'description'>): boolean {
  const loc = job.location ?? '';
  if (SPAIN_ONLY_PATTERNS.some((pattern) => pattern.test(loc))) {
    return true;
  }
  const titleText = job.title ?? '';
  if (SPAIN_ONLY_PATTERNS.some((pattern) => pattern.test(titleText))) {
    return true;
  }
  return false;
}

export function hasFakeRemoteRestriction(job: Pick<AtsJob, 'location' | 'title' | 'description'>): boolean {
  const locationAndTitle = `${job.location ?? ''} ${job.title ?? ''}`;
  if (FAKE_REMOTE_BLOCK_PATTERNS.some((pattern) => pattern.test(locationAndTitle))) {
    return true;
  }
  const descHead = (job.description ?? '').slice(0, 600);
  if (/\b(?:u\.?s\.?\s+only|united states only|must reside in the u\.?s\.?|uk only|united kingdom only|canada only)\b/i.test(descHead)) {
    return true;
  }
  return false;
}

export function classifyGeoTier(
  job: Pick<AtsJob, 'location' | 'workplaceType' | 'title' | 'description'>,
): { geoTier: GeoTier; isSpainOnly: boolean; isSpainOrEuRemote: boolean; isFakeRemoteBlocked: boolean } {
  const spainOnly = isSpainOnlyJob(job);
  if (spainOnly) {
    return {
      geoTier: 'spain',
      isSpainOnly: true,
      isSpainOrEuRemote: true,
      isFakeRemoteBlocked: false,
    };
  }

  const fakeBlocked = hasFakeRemoteRestriction(job);
  if (fakeBlocked) {
    return {
      geoTier: 'global',
      isSpainOnly: false,
      isSpainOrEuRemote: false,
      isFakeRemoteBlocked: true,
    };
  }

  const loc = job.location ?? '';
  const titleText = job.title ?? '';
  const isRemoteFlag = job.workplaceType === 'remote'
    || /\b(?:remote|remoto|teletrabajo|anywhere|worldwide)\b/i.test(`${loc} ${titleText}`);

  if (isRemoteFlag && (loc.trim() === '' || EU_REMOTE_PATTERNS.some((p) => p.test(`${loc} ${titleText}`)))) {
    return {
      geoTier: 'eu-remote',
      isSpainOnly: false,
      isSpainOrEuRemote: true,
      isFakeRemoteBlocked: false,
    };
  }

  return {
    geoTier: 'global',
    isSpainOnly: false,
    isSpainOrEuRemote: false,
    isFakeRemoteBlocked: false,
  };
}

export function isSpainMadridOrRemote(
  job: Pick<AtsJob, 'location' | 'workplaceType'> & Partial<Pick<AtsJob, 'title' | 'description'>>,
): boolean {
  return classifyGeoTier({
    location: job.location,
    workplaceType: job.workplaceType,
    title: job.title ?? '',
    description: job.description ?? '',
  }).isSpainOrEuRemote;
}

export function matchesGeoScope(scored: ScoredAtsJob, scope: GeoScope): boolean {
  if (scope === 'spain-only') return scored.isSpainOnly;
  if (scope === 'spain-eu-remote') return scored.isSpainOrEuRemote;
  return true;
}

export function detectSenioritySignal(job: Pick<AtsJob, 'title' | 'description'>): {
  signal: SenioritySignal;
  delta: number;
} {
  const title = job.title ?? '';
  const desc = (job.description ?? '').slice(0, 1200);
  if (SENIOR_STAFF_TITLE_PATTERNS.some((p) => p.test(title))
    || SENIOR_YEARS_PATTERNS.some((p) => p.test(`${title} ${desc}`))) {
    return { signal: 'senior-warning', delta: -25 };
  }
  if (JUNIOR_FRIENDLY_PATTERNS.some((p) => p.test(title))) {
    return { signal: 'junior', delta: 30 };
  }
  if (JUNIOR_FRIENDLY_PATTERNS.some((p) => p.test(desc))) {
    return { signal: 'junior', delta: 20 };
  }
  return { signal: 'neutral', delta: 0 };
}

export function isTechSoftwareRole(job: Pick<AtsJob, 'title' | 'department' | 'description'>): boolean {
  const titleAndDept = `${job.title} ${job.department ?? ''}`;
  if (TECH_TITLE_INDICATORS.some((indicator) => boundaryRegex(indicator).test(titleAndDept))) {
    return true;
  }
  const titleKeywordHits = ALL_TECH_HIGHLIGHT_KEYWORDS.filter((kw) => matchesKeyword(job.title, kw)).length;
  if (titleKeywordHits >= 1) return true;
  if (!job.description) return false;
  const descKeywordHits = ALL_TECH_HIGHLIGHT_KEYWORDS.filter((kw) => matchesKeyword(job.description, kw)).length;
  return descKeywordHits >= 2;
}

export function keywordsForPreset(preset: StackPresetId, customTags: readonly string[]): readonly string[] {
  switch (preset) {
    case 'sap':
      return SAP_KEYWORDS;
    case 'fullstack':
      return FULLSTACK_KEYWORDS;
    case 'custom':
      return customTags;
    case 'all-tech':
    default:
      return ALL_TECH_HIGHLIGHT_KEYWORDS;
  }
}

export function evaluateAtsJob(
  job: AtsJob,
  preset: StackPresetId,
  customTags: readonly string[],
): ScoredAtsJob {
  const activeKeywords = keywordsForPreset(preset, customTags);
  const titleText = job.title;
  const fullText = `${job.title}\n${job.department ?? ''}\n${job.description}`;

  const matchedKeywords: string[] = [];
  const titleMatches: string[] = [];
  for (const keyword of activeKeywords) {
    const inTitle = matchesKeyword(titleText, keyword);
    const inFull = inTitle || matchesKeyword(fullText, keyword);
    if (inFull) {
      matchedKeywords.push(keyword);
      if (inTitle) titleMatches.push(keyword);
    }
  }

  const techRole = isTechSoftwareRole(job);
  const hours = job.hoursSincePublished;
  const isFresh24h = hours !== null && hours <= 24;
  const isFresh48h = hours !== null && hours <= 48;
  const geo = classifyGeoTier(job);
  const seniority = detectSenioritySignal(job);

  let score = titleMatches.length * 25 + (matchedKeywords.length - titleMatches.length) * 10;
  if (techRole) score += 8;
  if (isFresh24h) score += 20;
  else if (isFresh48h) score += 12;
  else if (hours !== null && hours <= 168) score += 5;

  if (geo.isSpainOnly) {
    score += /\bmadrid\b/i.test(job.location ?? '') ? 25 : 18;
  } else if (geo.isSpainOrEuRemote) {
    score += 10;
  }
  score += seniority.delta;

  const isMatch = preset === 'all-tech'
    ? techRole || matchedKeywords.length > 0
    : matchedKeywords.length > 0;

  return {
    job,
    matchedKeywords,
    titleMatches,
    score,
    isMatch,
    isTechRole: techRole,
    isFresh24h,
    isFresh48h,
    isSpainOnly: geo.isSpainOnly,
    isSpainOrEuRemote: geo.isSpainOrEuRemote,
    isSpainOrRemote: geo.isSpainOrEuRemote,
    isFakeRemoteBlocked: geo.isFakeRemoteBlocked,
    geoTier: geo.geoTier,
    senioritySignal: seniority.signal,
  };
}

function geoTierRank(tier: GeoTier): number {
  if (tier === 'spain') return 0;
  if (tier === 'eu-remote') return 1;
  return 2;
}

export function sortScoredJobs(items: ScoredAtsJob[], mode: 'relevance' | 'newest'): ScoredAtsJob[] {
  const copy = [...items];
  copy.sort((a, b) => {
    if (mode === 'newest') {
      const ha = a.job.hoursSincePublished ?? Number.MAX_SAFE_INTEGER;
      const hb = b.job.hoursSincePublished ?? Number.MAX_SAFE_INTEGER;
      if (ha !== hb) return ha - hb;
      if (b.score !== a.score) return b.score - a.score;
      const ga = geoTierRank(a.geoTier);
      const gb = geoTierRank(b.geoTier);
      if (ga !== gb) return ga - gb;
      return a.job.title.localeCompare(b.job.title);
    }
    if (a.isMatch !== b.isMatch) return a.isMatch ? -1 : 1;
    if (b.score !== a.score) return b.score - a.score;
    // When scores are tied, Tier 1 (Spain / Madrid) sorts above Tier 2 (EU Remote).
    const ga = geoTierRank(a.geoTier);
    const gb = geoTierRank(b.geoTier);
    if (ga !== gb) return ga - gb;
    const ha = a.job.hoursSincePublished ?? Number.MAX_SAFE_INTEGER;
    const hb = b.job.hoursSincePublished ?? Number.MAX_SAFE_INTEGER;
    if (ha !== hb) return ha - hb;
    return a.job.companyName.localeCompare(b.job.companyName) || a.job.title.localeCompare(b.job.title);
  });
  return copy;
}

export function formatFreshness(
  hoursSincePublished: number | null,
  publishedAt: string | null,
  language: 'en' | 'es',
): FreshnessBadge | null {
  if (hoursSincePublished !== null) {
    if (hoursSincePublished < 1) {
      return {
        tier: 'hot',
        text: language === 'es' ? '🔥 Recién publicada' : '🔥 Just posted',
      };
    }
    if (hoursSincePublished <= 24) {
      return {
        tier: 'hot',
        text: language === 'es' ? `🔥 Hace ${hoursSincePublished}h` : `🔥 Posted ${hoursSincePublished}h ago`,
      };
    }
    if (hoursSincePublished <= 48) {
      return {
        tier: 'fresh',
        text: language === 'es' ? `⚡ Hace ${hoursSincePublished}h` : `⚡ Posted ${hoursSincePublished}h ago`,
      };
    }
    const days = Math.floor(hoursSincePublished / 24);
    if (days <= 14) {
      return {
        tier: 'recent',
        text: language === 'es'
          ? (days === 1 ? 'Hace 1 día' : `Hace ${days} días`)
          : (days === 1 ? 'Posted 1d ago' : `Posted ${days}d ago`),
      };
    }
    return {
      tier: 'normal',
      text: language === 'es' ? `Hace ${days} días` : `Posted ${days}d ago`,
    };
  }
  if (publishedAt) {
    const datePart = publishedAt.slice(0, 10);
    return {
      tier: 'normal',
      text: language === 'es' ? `Publicada ${datePart}` : `Posted ${datePart}`,
    };
  }
  return null;
}
