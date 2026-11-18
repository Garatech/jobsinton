import type { ProfessionalProfile } from '../profile/model';

export type StackPresetId = 'all-tech' | 'sap' | 'fullstack' | 'custom';

export interface StackPreset {
  id: StackPresetId;
  label: string;
  shortLabel: string;
  description: string;
  keywords: readonly string[];
}

export const CUSTOM_STACK_STORAGE_KEY = 'jobsinton.ats-custom-stack';

export const SAP_KEYWORDS = ['SAP', 'ABAP', 'S/4HANA', 'RAP', 'CAP', 'OData', 'Fiori', 'BTP'] as const;

export const FULLSTACK_KEYWORDS = [
  'Java', 'Spring Boot', 'Angular', 'Node.js', 'React', 'TypeScript', 'PostgreSQL', 'Docker',
] as const;

export const ALL_TECH_HIGHLIGHT_KEYWORDS = [
  ...SAP_KEYWORDS,
  ...FULLSTACK_KEYWORDS,
  'JavaScript', 'Python', 'Go', 'Kubernetes', 'AWS', 'SQL', 'GraphQL', 'REST', 'Git', 'CI/CD',
] as const;

export const STACK_PRESETS: readonly StackPreset[] = [
  {
    id: 'all-tech',
    label: 'All Tech / Software Roles',
    shortLabel: 'All Tech Roles',
    description: 'Broad software engineering, full-stack, backend, frontend, cloud, data, and SAP roles.',
    keywords: ALL_TECH_HIGHLIGHT_KEYWORDS,
  },
  {
    id: 'sap',
    label: 'Track A · SAP & Enterprise',
    shortLabel: 'Track A · SAP',
    description: 'SAP S/4HANA, ABAP OO, RAP, CAP, OData v4, Fiori, and SAP BTP roles.',
    keywords: SAP_KEYWORDS,
  },
  {
    id: 'fullstack',
    label: 'Track B · Full Stack (Java / JS)',
    shortLabel: 'Track B · Full Stack',
    description: 'Java Spring Boot, Angular, Node.js, React, TypeScript, PostgreSQL, and Docker roles.',
    keywords: FULLSTACK_KEYWORDS,
  },
  {
    id: 'custom',
    label: 'Custom Profile',
    shortLabel: 'Custom Profile',
    description: 'Define your own keyword tags or sync from your saved developer profile.',
    keywords: [],
  },
];

export const DEFAULT_CUSTOM_TAGS = ['Java', 'Spring Boot', 'TypeScript', 'React', 'SAP'] as const;

export function parsePresetId(raw: string | null): StackPresetId {
  if (raw === 'sap' || raw === 'fullstack' || raw === 'custom' || raw === 'all-tech') {
    return raw;
  }
  return 'all-tech';
}

export function loadCustomTags(profile: ProfessionalProfile | null): string[] {
  try {
    const raw = window.localStorage.getItem(CUSTOM_STACK_STORAGE_KEY);
    if (raw) {
      const parsed: unknown = JSON.parse(raw);
      if (Array.isArray(parsed)) {
        const clean = parsed
          .filter((item): item is string => typeof item === 'string')
          .map((item) => item.trim())
          .filter((item) => item.length > 0 && item.length <= 40);
        if (clean.length > 0) {
          return [...new Set(clean)].slice(0, 20);
        }
      }
    }
  } catch {
    // Ignore restricted storage and fall back to profile or default tags.
  }
  if (profile && profile.technologies.length > 0) {
    return [...profile.technologies];
  }
  return [...DEFAULT_CUSTOM_TAGS];
}

export function saveCustomTags(tags: string[]): boolean {
  try {
    window.localStorage.setItem(CUSTOM_STACK_STORAGE_KEY, JSON.stringify(tags.slice(0, 20)));
    return true;
  } catch {
    return false;
  }
}
