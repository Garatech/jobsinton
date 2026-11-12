import type { ProfessionalProfile } from '../profile/model';

export interface RankedOrder { ranked: string[]; unavailable: string[] }

/** A response must partition precisely this filtered set before it can replace ordinary search. */
export function parseRankedOrder(raw: unknown, requested: string[]): RankedOrder {
  if (typeof raw !== 'object' || raw === null || !('apiVersion' in raw) || raw.apiVersion !== 'ranking-api-v1'
    || !('rankingVersion' in raw) || raw.rankingVersion !== 'ranking-v1.0'
    || !('matchingVersion' in raw) || raw.matchingVersion !== 'matching-v1.0'
    || !('ranked' in raw) || !Array.isArray(raw.ranked)
    || !('unavailable' in raw) || !Array.isArray(raw.unavailable)
    || raw.ranked.length + raw.unavailable.length !== requested.length) throw new Error('Invalid ranking response.');
  const remaining = new Set(requested);
  for (const id of [...raw.ranked, ...raw.unavailable]) {
    if (typeof id !== 'string' || !remaining.delete(id)) throw new Error('Unexpected or repeated opportunity.');
  }
  if (remaining.size) throw new Error('Incomplete ranking.');
  return { ranked: raw.ranked, unavailable: raw.unavailable };
}

export async function fetchRankedOrder(profile: ProfessionalProfile, opportunityIds: string[], signal: AbortSignal) {
  const response = await fetch('/api/matching/rank', {
    method: 'POST', signal, cache: 'no-store',
    headers: { Accept: 'application/json', 'Content-Type': 'application/json' },
    body: JSON.stringify({ profile, opportunityIds }),
  });
  if (!response.ok) throw new Error('Ranking unavailable.');
  const raw = await response.text();
  if (raw.length > 65_536) throw new Error('Unexpected ranking response size.');
  return parseRankedOrder(JSON.parse(raw) as unknown, opportunityIds);
}
