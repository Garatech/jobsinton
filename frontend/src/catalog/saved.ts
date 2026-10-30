import { opportunities } from './data';

export const SAVED_STORAGE_KEY = 'jobsinton.saved-opportunities';
const known = new Set(opportunities.map(job => job.id));
export type SavedLoad = { ids: string[]; error: string };

/** Preserve unreadable records until an explicit user action replaces or removes them. */
export function loadSaved(): SavedLoad {
  try {
    const raw = localStorage.getItem(SAVED_STORAGE_KEY);
    if (raw === null) return { ids: [], error: '' };
    if (raw.length > 16384) throw new Error('Oversized record');
    const parsed: unknown = JSON.parse(raw);
    if (!parsed || typeof parsed !== 'object' || !('version' in parsed) || parsed.version !== 1 ||
      !('ids' in parsed) || !Array.isArray(parsed.ids) || parsed.ids.length > 500 ||
      !parsed.ids.every(id => typeof id === 'string' && id.length <= 100)) throw new Error('Invalid record');
    return { ids: [...new Set(parsed.ids.filter(id => known.has(id)))], error: '' };
  } catch {
    return { ids: [], error: 'Saved opportunities could not be read. Allow browser storage, then save an opportunity or clear the shortlist to recover.' };
  }
}
export function writeSaved(ids: string[]): boolean {
  try {
    localStorage.setItem(SAVED_STORAGE_KEY, JSON.stringify({ version: 1, ids: [...new Set(ids)].filter(id => known.has(id)) }));
    return true;
  } catch { return false; }
}
export function clearSaved(): boolean {
  try { localStorage.removeItem(SAVED_STORAGE_KEY); return true; }
  catch { return false; }
}
