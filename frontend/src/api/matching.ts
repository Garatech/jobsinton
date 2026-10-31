import type { ProfessionalProfile } from '../profile/model';

const outcomes = ['ALIGNED', 'DIFFERENT', 'UNKNOWN', 'NOT_ASSESSED'] as const;
const summaries = ['CONFLICT', 'NEEDS_INFORMATION', 'NO_KNOWN_CONFLICT', 'NOT_ASSESSED'] as const;
const dimensions = ['ROLE', 'TECHNOLOGY', 'EXPERIENCE', 'WORK_MODE', 'ATTENDANCE', 'OFFICE_LOCATION', 'RESIDENCE', 'SALARY', 'OTHER_CONDITIONS'] as const;
const reasons = ["ROLE_ALIGNED","ROLE_DIFFERENT","ROLE_UNKNOWN","TECHNOLOGY_CONFIRMED","TECHNOLOGY_UNCONFIRMED","TECHNOLOGIES_UNKNOWN","EXPERIENCE_SUFFICIENT","EXPERIENCE_BELOW_MINIMUM","EXPERIENCE_UNKNOWN","EXPERIENCE_SCOPE_UNCONFIRMED","MODE_ALIGNED","MODE_DIFFERENT","MODE_UNKNOWN","ATTENDANCE_WITHIN_LIMIT","ATTENDANCE_EXCEEDS_LIMIT","ATTENDANCE_UNKNOWN","ATTENDANCE_NOT_CONFIGURED","OFFICE_ALIGNED","OFFICE_DIFFERENT","OFFICE_UNKNOWN","OFFICE_NOT_APPLICABLE","RESIDENCE_ALLOWED","RESIDENCE_OUTSIDE_POLICY","RESIDENCE_UNRESTRICTED","RESIDENCE_POLICY_UNKNOWN","RESIDENCE_COUNTRY_UNKNOWN","SALARY_NOT_CONFIGURED","CONDITIONS_NOT_SUPPORTED"] as const;

export interface Assessment {
  criterion: string;
  dimension: typeof dimensions[number];
  kind: 'ADVISORY' | 'CONSTRAINT';
  outcome: typeof outcomes[number];
  reason: typeof reasons[number];
  explanation: string;
  evidence: { profileValues: string[]; opportunityValues: string[]; limitation: string };
}
export interface Comparison {
  apiVersion: 'matching-api-v1';
  opportunityId: string;
  evaluation: {
    rulesVersion: 'matching-v1.0';
    constraintSummary: typeof summaries[number];
    assessments: Assessment[];
  };
}

export class ComparisonError extends Error {}

const criteria: Record<string, Assessment['dimension']> = {
  role: 'ROLE', experience: 'EXPERIENCE', 'work-mode': 'WORK_MODE', attendance: 'ATTENDANCE',
  'office-location': 'OFFICE_LOCATION', residence: 'RESIDENCE', salary: 'SALARY', 'other-conditions': 'OTHER_CONDITIONS',
};
function record(value: unknown): value is Record<string, unknown> {
  return typeof value === 'object' && value !== null && !Array.isArray(value);
}
function text(value: unknown): value is string { return typeof value === 'string' && value.length > 0 && value.length <= 2000; }
function strings(value: unknown): value is string[] {
  return Array.isArray(value) && value.length <= 250 && value.every(text);
}
function oneOf<T extends string>(values: readonly T[], value: unknown): value is T {
  return typeof value === 'string' && values.includes(value as T);
}
function assessment(value: unknown): value is Assessment {
  if (!record(value) || !text(value.criterion) || !oneOf(dimensions, value.dimension)
    || !oneOf(['ADVISORY', 'CONSTRAINT'], value.kind) || !oneOf(outcomes, value.outcome)
    || !oneOf(reasons, value.reason) || !text(value.explanation) || !record(value.evidence)
    || !strings(value.evidence.profileValues) || !strings(value.evidence.opportunityValues)
    || !text(value.evidence.limitation)) return false;
  const expected = value.criterion === 'technologies' || value.criterion.startsWith('technology:')
    ? 'TECHNOLOGY' : criteria[value.criterion];
  const kind = ['ATTENDANCE', 'RESIDENCE'].includes(value.dimension) ? 'CONSTRAINT' : 'ADVISORY';
  return expected === value.dimension && value.kind === kind;
}

/** Validate the whole response before it can replace loading or error feedback. */
export function parseComparison(value: unknown, opportunityId: string): Comparison {
  if (!record(value) || value.apiVersion !== 'matching-api-v1' || value.opportunityId !== opportunityId
    || !record(value.evaluation) || value.evaluation.rulesVersion !== 'matching-v1.0'
    || !oneOf(summaries, value.evaluation.constraintSummary) || !Array.isArray(value.evaluation.assessments)
    || value.evaluation.assessments.length > 110 || !value.evaluation.assessments.every(assessment)) {
    throw new ComparisonError('The comparison response could not be verified. Please try again.');
  }
  const assessments = value.evaluation.assessments;
  const ids = new Set(assessments.map(a => a.criterion));
  if (ids.size !== assessments.length || !Object.keys(criteria).every(id => ids.has(id))
    || !assessments.some(a => a.dimension === 'TECHNOLOGY')) {
    throw new ComparisonError('The comparison response is incomplete. Please try again.');
  }
  return value as unknown as Comparison;
}

export async function fetchComparison(opportunityId: string, profile: ProfessionalProfile, signal: AbortSignal): Promise<Comparison> {
  const response = await fetch('/api/matching/evaluate', {
    method: 'POST', signal, cache: 'no-store',
    headers: { Accept: 'application/json', 'Content-Type': 'application/json' },
    body: JSON.stringify({ opportunityId, profile }),
  });
  if (!response.ok) {
    if (response.status === 422) throw new ComparisonError('Review your saved profile and try again.');
    if (response.status === 404) throw new ComparisonError('This opportunity is not available for comparison. Refresh the page and try again.');
    throw new ComparisonError('The comparison is unavailable. Please try again.');
  }
  const raw = await response.text();
  if (raw.length > 131_072) throw new ComparisonError('The comparison response could not be verified. Please try again.');
  return parseComparison(JSON.parse(raw) as unknown, opportunityId);
}
