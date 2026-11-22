import type { AtsJob } from '../api/ats';
import {
  AI_ML_KEYWORDS,
  ALL_TECH_HIGHLIGHT_KEYWORDS,
  CLOUD_DEVOPS_KEYWORDS,
  FULLSTACK_KEYWORDS,
  PYTHON_DATA_KEYWORDS,
  QA_CYBER_KEYWORDS,
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
  'vue.js': ['vue.js', 'vuejs', 'vue js', 'vue'],
  'node.js': ['node.js', 'nodejs', 'node js', 'node'],
  'next.js': ['next.js', 'nextjs', 'next js'],
  react: ['react', 'react.js', 'reactjs', 'react native'],
  typescript: ['typescript'],
  javascript: ['javascript'],
  'c#': ['c#', 'csharp', 'c-sharp'],
  '.net': ['.net', 'dotnet', 'asp.net', '.net core'],
  php: ['php', 'laravel', 'symfony'],
  kotlin: ['kotlin'],
  swift: ['swift'],
  flutter: ['flutter'],
  postgresql: ['postgresql', 'postgres'],
  mysql: ['mysql', 'mariadb'],
  mongodb: ['mongodb', 'mongo'],
  docker: ['docker'],
  python: ['python'],
  'data analyst': ['data analyst', 'analista de datos', 'data analytics', 'product analyst', 'bi analyst', 'analytics engineer'],
  'data engineer': ['data engineer', 'ingeniero de datos', 'ingeniera de datos', 'data engineering'],
  'data scientist': ['data scientist', 'científico de datos', 'cientifico de datos', 'data science'],
  'business intelligence': ['business intelligence', 'inteligencia de negocio', 'bi developer', 'consultor bi'],
  'power bi': ['power bi', 'powerbi'],
  tableau: ['tableau'],
  looker: ['looker', 'looker studio'],
  pandas: ['pandas', 'numpy'],
  django: ['django'],
  fastapi: ['fastapi', 'fast api'],
  flask: ['flask'],
  spark: ['spark', 'apache spark'],
  pyspark: ['pyspark'],
  databricks: ['databricks'],
  snowflake: ['snowflake'],
  bigquery: ['bigquery', 'big query'],
  airflow: ['airflow', 'apache airflow'],
  dbt: ['dbt'],
  kafka: ['kafka', 'apache kafka'],
  etl: ['etl', 'elt', 'data pipeline', 'data pipelines'],
  'ai engineer': ['ai engineer', 'ingeniero de ia', 'ingeniero ia', 'ai software engineer', 'artificial intelligence engineer', 'applied ai'],
  'machine learning': ['machine learning', 'ml engineer', 'machine learning engineer', 'aprendizaje automático'],
  'generative ai': ['generative ai', 'genai', 'gen ai', 'ia generativa'],
  llm: ['llm', 'llms', 'large language model', 'large language models', 'agentic ai', 'ai agents'],
  nlp: ['nlp', 'natural language processing', 'procesamiento de lenguaje natural'],
  'computer vision': ['computer vision', 'visión artificial', 'vision artificial', 'opencv'],
  'deep learning': ['deep learning', 'neural networks'],
  pytorch: ['pytorch'],
  tensorflow: ['tensorflow'],
  'scikit-learn': ['scikit-learn', 'sklearn', 'scikit learn'],
  langchain: ['langchain', 'langgraph', 'llamaindex'],
  rag: ['rag', 'retrieval augmented generation', 'vector database'],
  mlops: ['mlops', 'llmops'],
  'data science': ['data science', 'data scientist', 'ciencia de datos'],
  openai: ['openai'],
  'hugging face': ['hugging face', 'huggingface', 'transformers'],
  aws: ['aws', 'amazon web services'],
  azure: ['azure', 'microsoft azure'],
  gcp: ['gcp', 'google cloud', 'google cloud platform'],
  kubernetes: ['kubernetes', 'k8s', 'openshift', 'helm'],
  terraform: ['terraform', 'opentofu', 'iac'],
  ansible: ['ansible'],
  'ci/cd': ['ci/cd', 'cicd', 'github actions', 'jenkins', 'gitlab ci', 'azure devops'],
  linux: ['linux', 'ubuntu', 'debian', 'redhat', 'rhel', 'bash'],
  devops: ['devops', 'devsecops'],
  sre: ['sre', 'site reliability'],
  'platform engineer': ['platform engineer', 'platform engineering', 'ingeniero de plataforma'],
  'systems engineer': ['systems engineer', 'system engineer', 'ingeniero de sistemas', 'técnico de sistemas', 'tecnico de sistemas'],
  sysadmin: ['sysadmin', 'system administrator', 'systems administrator', 'administrador de sistemas'],
  'cloud engineer': ['cloud engineer', 'cloud architect', 'ingeniero cloud', 'arquitecto cloud'],
  qa: ['qa', 'quality assurance', 'qa engineer', 'qa analyst'],
  'test automation': ['test automation', 'qa automation', 'automated testing', 'automatización de pruebas'],
  sdet: ['sdet', 'software development engineer in test'],
  playwright: ['playwright'],
  cypress: ['cypress'],
  selenium: ['selenium', 'appium'],
  cybersecurity: ['cybersecurity', 'ciberseguridad', 'cyber security', 'infosec', 'information security'],
  'security engineer': ['security engineer', 'ingeniero de seguridad', 'security analyst', 'analista de seguridad', 'cloud security'],
  soc: ['soc', 'soc analyst', 'soc engineer', 'security operations'],
  siem: ['siem', 'splunk', 'sentinel', 'crowdstrike'],
  pentesting: ['pentesting', 'penetration testing', 'pentester', 'ethical hacking', 'red team'],
  appsec: ['appsec', 'application security'],
  sap: ['sap'],
  abap: ['abap', 'abap oo', 'abap cloud'],
  's/4hana': ['s/4hana', 's/4 hana', 's4hana', 's4/hana', 'sap hana'],
  rap: ['rap', 'abap rap'],
  cap: ['cap', 'sap cap', 'cloud application programming'],
  odata: ['odata', 'odata v4', 'odata v2'],
  fiori: ['fiori', 'sapui5', 'ui5'],
  btp: ['btp', 'sap btp'],
  'sap hana': ['sap hana', 'hana db'],
  'fi/co': ['fi/co', 'fico', 'sap fi', 'sap co', 'sap mm', 'sap sd', 'sap pp', 'sap ewm', 'sap tm', 'sap bw'],
  successfactors: ['successfactors', 'success factors', 'sap hcm'],
  salesforce: ['salesforce', 'apex', 'lwc', 'salesforce developer', 'consultor salesforce'],
  'dynamics 365': ['dynamics 365', 'microsoft dynamics', 'navision', 'business central', 'power platform', 'power apps'],
  servicenow: ['servicenow', 'service now'],
  erp: ['erp', 'odoo', 'netsuite', 'consultor erp'],
};

const TECH_TITLE_INDICATORS = [
  'engineer', 'developer', 'software', 'backend', 'back-end', 'frontend', 'front-end',
  'full stack', 'full-stack', 'fullstack', 'devops', 'sre', 'platform', 'architect',
  'data', 'data analyst', 'data scientist', 'data engineer', 'analytics', 'business intelligence',
  'cloud', 'sap', 'abap', 'fiori', 'salesforce', 'dynamics', 'servicenow', 'erp',
  'qa', 'sdet', 'automation', 'tester', 'security', 'cybersecurity', 'ciberseguridad',
  'pentester', 'soc analyst', 'sysadmin', 'system administrator', 'network engineer',
  'mobile', 'ios', 'android', 'web', 'systems', 'infrastructure', 'ml', 'ai', 'machine learning',
  'python', 'desarrollador', 'desarrolladora', 'programador', 'programadora', 'ingeniero',
  'ingeniera', 'analista de datos', 'científico de datos', 'cientifico de datos',
  'administrador de sistemas', 'técnico de sistemas', 'tecnico de sistemas',
  'consultor sap', 'consultora sap', 'consultor', 'arquitecto', 'tech lead',
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
  /\bu\.?s\.?a\.?\b/i,
  /\bunited states\b/i,
  /\bremote\s*[-–(/,:|]+\s*(?:u\.?s\.?a?\.?|canada|ca|uk|u\.?k\.?|apac|latam|india|brazil|mexico|australia)\b/i,
  /\bnorth america\b/i,
  /\bcanada(?:\s+only)?\b/i,
  // Canadian cities, provinces & ", CA" country/state code (e.g. "Toronto, CA, Remote" or "San Francisco, CA")
  /\b(?:toronto|vancouver|montreal|montréal|ottawa|calgary|edmonton|waterloo|winnipeg|quebec|québec|halifax|ontario|british columbia|alberta)\b/i,
  /,\s*(?:CA|ON|BC|QC|AB)(?:\b|,|\s|-)/,
  // US 2-letter state codes after comma (e.g. "Austin, TX", "New York, NY", "Seattle, WA", "Boston, MA")
  /,\s*(?:AL|AK|AZ|AR|CA|CO|CT|DE|FL|GA|HI|ID|IL|IN|IA|KS|KY|LA|ME|MD|MA|MI|MN|MS|MO|MT|NE|NV|NH|NJ|NM|NY|NC|ND|OH|OK|OR|PA|RI|SC|SD|TN|TX|UT|VT|VA|WA|WV|WI|WY|DC)(?:\b|,|\s|-)/,
  // Major US tech hub cities
  /\b(?:san francisco|new york|nyc|seattle|austin|boston|chicago|los angeles|denver|atlanta|miami|dallas|houston|silicon valley|palo alto|mountain view|menlo park|cupertino|redmond|washington,?\s*d\.?c\.?)\b/i,
  /\blatam\b/i,
  /\blatin america\b/i,
  /\bbrazil\b/i,
  /\bmexico\b/i,
  /\bargentina\b/i,
  /\bcolombia\b/i,
  /\bchile\b/i,
  /\bapac\b/i,
  /\bindia\b/i,
  /\bsingapore\b/i,
  /\bjapan\b/i,
  /\bu\.?k\.?\s+only\b/i,
  /\bunited kingdom\s+only\b/i,
  /\baustralia\b/i,
  /\bnew zealand\b/i,
  /\b(?:pst|pdt|est|edt|cst|cdt|mst|mdt)\b/i,
  /\bmust reside in (?:the )?(?:u\.?s\.?|united states|canada|uk|united kingdom)\b/i,
  /\bauthorized to work in (?:the )?(?:u\.?s\.?|united states|canada)\b/i,
];

const EU_COUNTRY_AND_CITY_PATTERNS = [
  /\beurope\b/i,
  /\beuropean union\b/i,
  /\beu\b/i,
  /\bcet\b/i,
  /\bcest\b/i,
  /\bdach\b/i,
  /\bnordics\b/i,
  /\bbenelux\b/i,
  /\butc\s*\+\s*[12]\b/i,
  /\b(?:germany|deutschland|berlin|munich|münchen|hamburg|frankfurt|stuttgart|cologne|köln|düsseldorf|leipzig|dresden|karlsruhe|nuremberg|nürnberg|walldorf)\b/i,
  /\b(?:france|paris|lyon|toulouse|nantes|bordeaux|lille|marseille|nice|sophia antipolis|grenoble|rennes|strasbourg|montpellier)\b/i,
  /\b(?:portugal|lisbon|lisboa|porto|braga|coimbra|aveiro|faro)\b/i,
  /\b(?:italy|italia|milan|milano|rome|roma|turin|torino|bologna|florence|firenze|naples|napoli|padua|padova|genoa|verona)\b/i,
  /\b(?:netherlands|holland|amsterdam|rotterdam|utrecht|eindhoven|the hague|den haag|delft|groningen)\b/i,
  /\b(?:ireland|dublin|cork|galway|limerick)\b/i,
  /\b(?:poland|polska|warsaw|warszawa|krakow|kraków|cracow|wroclaw|wrocław|gdansk|gdańsk|poznan|poznań|katowice|lodz)\b/i,
  /\b(?:sweden|sverige|stockholm|gothenburg|göteborg|malmö|malmo|uppsala|lund)\b/i,
  /\b(?:austria|österreich|vienna|wien|graz|linz|salzburg|innsbruck)\b/i,
  /\b(?:belgium|brussels|bruxelles|antwerp|ghent|leuven)\b/i,
  /\b(?:switzerland|schweiz|suisse|zurich|zürich|geneva|genève|basel|lausanne|bern|zug)\b/i,
  /\b(?:denmark|copenhagen|københavn|aarhus|odense)\b/i,
  /\b(?:finland|helsinki|espoo|tampere|oulu|turku)\b/i,
  /\b(?:norway|oslo|bergen|trondheim|stavanger)\b/i,
  /\b(?:czech|czechia|prague|praha|brno)\b/i,
  /\b(?:romania|bucharest|bucurești|cluj|timisoara|iasi)\b/i,
  /\b(?:hungary|budapest|debrecen|greece|athens|thessaloniki|bulgaria|sofia|croatia|zagreb|estonia|tallinn|lithuania|vilnius|latvia|riga|slovenia|ljubljana|slovakia|bratislava|luxembourg|malta|cyprus|nicosia|limassol)\b/i,
];

const EU_REMOTE_PATTERNS = [
  ...EU_COUNTRY_AND_CITY_PATTERNS,
  /\bemea\b/i,
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
  const descHead = (job.description ?? '').slice(0, 800);
  if (/\b(?:u\.?s\.?\s+only|united states only|must reside in (?:the )?(?:u\.?s\.?|united states|canada)|authorized to work in (?:the )?(?:u\.?s\.?|united states|canada)|uk only|united kingdom only|canada only|north america only)\b/i.test(descHead)) {
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
  const combined = `${loc} ${titleText}`;
  const isRemoteFlag = job.workplaceType === 'remote'
    || /\b(?:remote|remoto|teletrabajo|anywhere|worldwide)\b/i.test(combined);
  const isEuropeanHub = EU_COUNTRY_AND_CITY_PATTERNS.some((p) => p.test(combined));

  if (isEuropeanHub || (isRemoteFlag && (loc.trim() === '' || EU_REMOTE_PATTERNS.some((p) => p.test(combined))))) {
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
    case 'python-data':
      return PYTHON_DATA_KEYWORDS;
    case 'ai-ml':
      return AI_ML_KEYWORDS;
    case 'cloud-devops':
      return CLOUD_DEVOPS_KEYWORDS;
    case 'qa-cyber':
      return QA_CYBER_KEYWORDS;
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
