/** Profile v1 is local to one browser; it is not a user account or hiring score. */
export const ROLES = {
  backend: 'Backend developer',
  frontend: 'Frontend developer',
  'full-stack': 'Full stack developer',
  sap: 'SAP developer',
} as const;

export const WORK_MODES = {
  remote: 'Remote',
  hybrid: 'Hybrid',
  onsite: 'On-site',
} as const;

export interface ProfessionalProfile {
  role: keyof typeof ROLES;
  technologies: string[];
  experienceYears: number;
  country: string;
  city: string;
  workMode: keyof typeof WORK_MODES;
  /** Remote profiles always store null; other modes store an integer from 1 to 5. */
  maxOnsiteDays: number | null;
}

/** Keep number inputs as strings so an empty field is never silently converted to zero. */
export interface ProfileDraft {
  role: string;
  technologies: string[];
  experienceYears: string;
  country: string;
  city: string;
  workMode: string;
  maxOnsiteDays: string;
}

export type ProfileErrors = Partial<Record<keyof ProfileDraft, string>>;
type Validation = { ok: true; profile: ProfessionalProfile } | { ok: false; errors: ProfileErrors };

export function emptyDraft(): ProfileDraft {
  return { role: '', technologies: [], experienceYears: '', country: '', city: '', workMode: '', maxOnsiteDays: '' };
}

export function toDraft(profile: ProfessionalProfile): ProfileDraft {
  return {
    ...profile,
    technologies: [...profile.technologies],
    experienceYears: String(profile.experienceYears),
    maxOnsiteDays: profile.maxOnsiteDays === null ? '' : String(profile.maxOnsiteDays),
  };
}

export function cleanText(value: string): string {
  return value.trim().replace(/\s+/g, ' ');
}

export function technologyError(value: string, existing: string[]): string | undefined {
  const technology = cleanText(value);
  if (!technology) return 'Enter a technology to add.';
  if (technology.length > 40) return 'Use 40 characters or fewer per technology.';
  if (existing.some((item) => cleanText(item).toLowerCase() === technology.toLowerCase())) {
    return 'This technology is already in your profile.';
  }
  if (existing.length >= 20) return 'Add up to 20 technologies.';
  return undefined;
}

function isRole(value: string): value is ProfessionalProfile['role'] {
  return Object.hasOwn(ROLES, value);
}

function isWorkMode(value: string): value is ProfessionalProfile['workMode'] {
  return Object.hasOwn(WORK_MODES, value);
}

/** One validation boundary is shared by form submission and saved-data recovery. */
export function validateProfile(draft: ProfileDraft): Validation {
  const errors: ProfileErrors = {};
  const technologies = draft.technologies.map(cleanText);
  const country = cleanText(draft.country);
  const city = cleanText(draft.city);
  const years = Number(draft.experienceYears);
  const days = Number(draft.maxOnsiteDays);

  if (!isRole(draft.role)) errors.role = 'Choose a target role.';
  if (technologies.length === 0) errors.technologies = 'Add at least one technology.';
  else if (technologies.length > 20) errors.technologies = 'Add up to 20 technologies.';
  else if (technologies.some((item, index) => technologyError(item, technologies.slice(0, index)))) {
    errors.technologies = 'Use unique technologies with 1–40 characters each.';
  }
  if (!/^\d+$/.test(draft.experienceYears) || !Number.isInteger(years) || years < 0 || years > 60) {
    errors.experienceYears = 'Enter a whole number of years from 0 to 60.';
  }
  if (!country || country.length > 80) errors.country = 'Enter a country using 1–80 characters.';
  if (!city || city.length > 80) errors.city = 'Enter a city using 1–80 characters.';
  if (!isWorkMode(draft.workMode)) errors.workMode = 'Choose a work mode.';
  if (isWorkMode(draft.workMode) && draft.workMode !== 'remote'
    && (!/^\d+$/.test(draft.maxOnsiteDays) || !Number.isInteger(days) || days < 1 || days > 5)) {
    errors.maxOnsiteDays = 'Choose a maximum of 1 to 5 on-site days per week.';
  }

  if (Object.keys(errors).length || !isRole(draft.role) || !isWorkMode(draft.workMode)) {
    return { ok: false, errors };
  }
  return {
    ok: true,
    profile: {
      role: draft.role, technologies, experienceYears: years, country, city,
      workMode: draft.workMode, maxOnsiteDays: draft.workMode === 'remote' ? null : days,
    },
  };
}

/** localStorage is untrusted input: reject malformed or unsupported records before rendering. */
export function parseSavedProfile(raw: string): ProfessionalProfile | null {
  try {
    if (raw.length > 32_768) return null;
    const envelope: unknown = JSON.parse(raw);
    if (!isRecord(envelope) || envelope.version !== 1 || !isRecord(envelope.profile)) return null;
    const p = envelope.profile;
    if (typeof p.role !== 'string' || !isRole(p.role)
      || !Array.isArray(p.technologies) || !p.technologies.every((item) => typeof item === 'string')
      || typeof p.experienceYears !== 'number' || !Number.isInteger(p.experienceYears)
      || typeof p.country !== 'string' || typeof p.city !== 'string'
      || typeof p.workMode !== 'string' || !isWorkMode(p.workMode)
      || (p.workMode === 'remote' ? p.maxOnsiteDays !== null
        : typeof p.maxOnsiteDays !== 'number' || !Number.isInteger(p.maxOnsiteDays))) return null;
    const result = validateProfile({
      role: p.role, technologies: p.technologies,
      experienceYears: String(p.experienceYears), country: p.country, city: p.city,
      workMode: p.workMode, maxOnsiteDays: p.maxOnsiteDays === null ? '' : String(p.maxOnsiteDays),
    });
    return result.ok ? result.profile : null;
  } catch {
    return null;
  }
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === 'object' && value !== null && !Array.isArray(value);
}
