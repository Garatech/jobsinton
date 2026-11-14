const root = '/api/providers/adzuna';

export async function localSearchAvailable(signal: AbortSignal): Promise<boolean> {
  if (!['127.0.0.1', 'localhost', '[::1]'].includes(location.hostname)) return false;
  try {
    const res = await fetch(root + '/availability', { signal, cache: 'no-store', headers: { Accept: 'application/json' } });
    return res.ok && res.headers.get('Content-Type')?.includes('application/json') === true
      && (await res.json())?.source === 'Adzuna';
  } catch { return false; }
}

export interface Listing {
  id: string; title: string; referral: string; company: string | null;
  locationLabel: string | null; excerpt: string | null;
  salary: { minimum: number | null; maximum: number | null; origin: string } | null;
}
export interface SearchPage { listings: Listing[]; received: number; rejected: number; page: number }

function record(value: unknown): value is Record<string, unknown> {
  return typeof value === 'object' && value !== null;
}
const nullableText = (v: unknown) => v === null || typeof v === 'string';
const bound = (v: unknown) => v === null || (typeof v === 'number' && Number.isFinite(v) && v > 0);
function listing(v: unknown): v is Listing {
  if (!record(v) || typeof v.id !== 'string' || !v.id.startsWith('adzuna:') || typeof v.title !== 'string' || !v.title.trim()
    || !nullableText(v.company) || !nullableText(v.locationLabel) || !nullableText(v.excerpt)
    || typeof v.referral !== 'string') return false;
  try {
    const u = new URL(v.referral);
    if (u.protocol !== 'https:' || u.username || u.password || u.port || u.pathname === '/'
      || !['adzuna.es','adzuna.com.es','adzuna.co.uk','adzuna.com'].includes(u.hostname.replace(/^www\./, ''))
      || [...u.searchParams.keys()].some(k => /^app_(id|key)$/i.test(k))) return false;
  } catch { return false; }
  const salary = v.salary;
  return salary === null || (record(salary) && bound(salary.minimum) && bound(salary.maximum)
    && typeof salary.origin === 'string' && ['ADVERTISED','PREDICTED','UNSPECIFIED'].includes(salary.origin));
}

const errors: Record<string,string> = {
  NOT_CONFIGURED: 'Adzuna credentials are missing from the local backend.',
  PROVIDER_ACCESS_DENIED: 'Adzuna did not accept the backend credentials.',
  PROVIDER_RATE_LIMITED: 'Adzuna’s request limit has been reached. Check the account before searching again.',
  PROVIDER_TIMEOUT: 'Adzuna took too long to respond.',
  INVALID_SEARCH: 'Enter a role and location of up to 100 characters each.',
  INVALID_PROVIDER_RESPONSE: 'Adzuna returned an unreadable page.',
  LOCAL_ONLY: 'Open JobSinton on the local backend to search Adzuna.',
};

export async function searchAdzuna(what: string, where: string, page: number, signal: AbortSignal): Promise<SearchPage> {
  const res = await fetch(root + '/search', {
    method: 'POST', signal, cache: 'no-store',
    headers: { Accept: 'application/json', 'Content-Type': 'application/json' },
    body: JSON.stringify({ what, where, page }),
  });
  const data: unknown = await res.json().catch(() => null);
  if (!res.ok) throw new Error(record(data) && typeof data.code === 'string' && Object.hasOwn(errors, data.code)
    ? errors[data.code] : 'Adzuna search is unavailable.');
  if (!record(data) || data.source !== 'Adzuna' || data.page !== page || !Array.isArray(data.listings)
    || !data.listings.every(listing) || typeof data.received !== 'number' || !Number.isInteger(data.received)
    || data.received < 0 || data.received > 10 || typeof data.rejected !== 'number' || typeof data.duplicates !== 'number'
    || !Number.isInteger(data.rejected) || !Number.isInteger(data.duplicates) || data.rejected < 0 || data.duplicates < 0
    || data.received !== data.listings.length + data.rejected + data.duplicates
    || new Set(data.listings.map(item => item.id)).size !== data.listings.length)
    throw new Error('The search response could not be verified.');
  return { page, received: data.received, rejected: data.rejected, listings: data.listings };
}
