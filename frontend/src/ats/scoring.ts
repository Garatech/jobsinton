import type { AtsJob } from '../api/ats';
import {
  ALL_TECH_HIGHLIGHT_KEYWORDS,
  FULLSTACK_KEYWORDS,
  SAP_KEYWORDS,
  type StackPresetId,
} from './presets';

export interface ScoredAtsJob {
  job: AtsJob;
  matchedKeywords: string[];
  titleMatches: string[];
  score: number;
  isMatch: boolean;
  isTechRole: boolean;
  isFresh24h: boolean;
  isFresh48h: boolean;
  isSpainOrRemote: boolean;
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
  'ingeniera', 'consultor sap', 'consultora sap', 'arquitecto', 'devops', 'tech lead',
];

const SPAIN_OR_REMOTE_PATTERNS = [
  /\bmadrid\b/i,
  /\bspain\b/i,
  /\bespa[nñ]a\b/i,
  /\bbarcelona\b/i,
  /\bvalencia\b/i,
  /\bbilbao\b/i,
  /\bsevilla\b/i,
  /\bm[aá]laga\b/i,
  /\bzaragoza\b/i,
  /\balicante\b/i,
  /\bcoru[nñ]a\b/i,
  /\bremote\b/i,
  /\bremoto\b/i,
  /\bteletrabajo\b/i,
  /\banywhere\b/i,
  /\bdistributed\b/i,
  /\bemea\b/i,
  /\beurope\s*\(?\s*remote/i,
  /\bremote\s*[-–,/]?\s*(eu|europe|emea|spain|es)\b/i,
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

export function isSpainMadridOrRemote(job: Pick<AtsJob, 'location' | 'workplaceType'>): boolean {
  if (job.workplaceType === 'remote') return true;
  if (!job.location) return false;
  return SPAIN_OR_REMOTE_PATTERNS.some((pattern) => pattern.test(job.location!));
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
  const spainOrRemote = isSpainMadridOrRemote(job);

  let score = titleMatches.length * 25 + (matchedKeywords.length - titleMatches.length) * 10;
  if (techRole) score += 8;
  if (isFresh24h) score += 20;
  else if (isFresh48h) score += 12;
  else if (hours !== null && hours <= 168) score += 5;
  if (spainOrRemote) score += 6;

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
    isSpainOrRemote: spainOrRemote,
  };
}

export function sortScoredJobs(items: ScoredAtsJob[], mode: 'relevance' | 'newest'): ScoredAtsJob[] {
  const copy = [...items];
  copy.sort((a, b) => {
    if (mode === 'newest') {
      const ha = a.job.hoursSincePublished ?? Number.MAX_SAFE_INTEGER;
      const hb = b.job.hoursSincePublished ?? Number.MAX_SAFE_INTEGER;
      if (ha !== hb) return ha - hb;
      if (b.score !== a.score) return b.score - a.score;
      return a.job.title.localeCompare(b.job.title);
    }
    if (a.isMatch !== b.isMatch) return a.isMatch ? -1 : 1;
    if (b.score !== a.score) return b.score - a.score;
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
