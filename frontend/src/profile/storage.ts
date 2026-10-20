import { parseSavedProfile, type ProfessionalProfile } from './model';

export const PROFILE_STORAGE_KEY = 'jobsinton.professional-profile';
export type ProfileLoad =
  | { kind: 'loaded'; profile: ProfessionalProfile }
  | { kind: 'empty' | 'invalid' | 'unavailable' };

/** Storage access itself can throw in restricted browsers, even before getItem runs. */
export function loadProfile(): ProfileLoad {
  try {
    const raw = window.localStorage.getItem(PROFILE_STORAGE_KEY);
    if (raw === null) return { kind: 'empty' };
    const profile = parseSavedProfile(raw);
    return profile ? { kind: 'loaded', profile } : { kind: 'invalid' };
  } catch {
    return { kind: 'unavailable' };
  }
}

/** Report success only after the browser accepts the write; never silently fall back to memory. */
export function saveProfile(profile: ProfessionalProfile): boolean {
  try {
    window.localStorage.setItem(PROFILE_STORAGE_KEY, JSON.stringify({ version: 1, profile }));
    return true;
  } catch {
    return false;
  }
}

export function removeProfile(): boolean {
  try {
    window.localStorage.removeItem(PROFILE_STORAGE_KEY);
    return true;
  } catch {
    return false;
  }
}
