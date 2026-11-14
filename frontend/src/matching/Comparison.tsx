import { reasonSpanish } from '../i18n/reasons';
import { useLanguage } from '../i18n/Language';
import { useEffect, useState } from 'react';
import { ComparisonError, fetchComparison, type Assessment, type Comparison as Result } from '../api/matching';
import type { Opportunity } from '../catalog/data';
import type { ProfessionalProfile } from '../profile/model';

type State = { kind: 'loading' } | { kind: 'ready'; result: Result } | { kind: 'error'; message: string };
const labels: Record<Assessment['dimension'], string> = {
  ROLE: 'Role', TECHNOLOGY: 'Technology', EXPERIENCE: 'Experience', WORK_MODE: 'Work mode',
  ATTENDANCE: 'Office days', OFFICE_LOCATION: 'Office location', RESIDENCE: 'Residence',
  SALARY: 'Salary', OTHER_CONDITIONS: 'Other conditions',
};
const summary = {
  CONFLICT: 'A stated limit is not met.',
  NEEDS_INFORMATION: 'Some conditions still need confirmation.',
  NO_KNOWN_CONFLICT: 'No conflict in the limits checked.',
  NOT_ASSESSED: 'No limits have been assessed.',
};

function Explanation({ assessment }: { assessment: Assessment }) {
  const { t, language } = useLanguage();
  const name = assessment.dimension === 'TECHNOLOGY'
    ? assessment.evidence.opportunityValues.join(', ') || t('Technologies') : t(labels[assessment.dimension]);
  return <li data-criterion={assessment.criterion} data-outcome={assessment.outcome}>
    <strong>{name}</strong> — {language === 'es' ? reasonSpanish[assessment.reason] : assessment.explanation}
  </li>;
}

/** Mounted with an identity key so a different profile cannot render old results for a frame. */
function SavedComparison({ job, profile }: { job: Opportunity; profile: ProfessionalProfile }) {
  const { t } = useLanguage();
  const [state, setState] = useState<State>({ kind: 'loading' });
  const [attempt, setAttempt] = useState(0);
  useEffect(() => {
    const controller = new AbortController();
    let active = true;
    const timer = window.setTimeout(() => {
      if (!active) return;
      active = false;
      controller.abort();
      setState({ kind: 'error', message: 'The comparison took too long. Please try again.' });
    }, 8000);
    fetchComparison(job.id, profile, controller.signal).then(result => {
      if (active) setState({ kind: 'ready', result });
    }).catch((error: unknown) => {
      if (active) setState({ kind: 'error', message: error instanceof ComparisonError
        ? error.message : 'The comparison is unavailable. Please try again.' });
    }).finally(() => window.clearTimeout(timer));
    return () => { active = false; controller.abort(); window.clearTimeout(timer); };
  }, [job.id, profile, attempt]);

  if (state.kind === 'loading') return <p role="status">{t("Comparing your saved profile…")}</p>;
  if (state.kind === 'error') return <div>
    <p role="alert">{t(state.message)}</p>
    <button className="secondary-button comparison-retry" type="button" onClick={() => {
      setState({ kind: 'loading' }); setAttempt(value => value + 1);
    }}>{t("Retry comparison")}</button>
    <p className="field-hint">{t("You can still browse and save this opportunity.")}</p>
  </div>;
  const assessments = state.result.evaluation.assessments;
  const known = assessments.filter(a => a.dimension !== 'TECHNOLOGY' && (a.outcome === 'ALIGNED' || a.outcome === 'DIFFERENT'));
  const shared = assessments.filter(a => a.reason === 'TECHNOLOGY_CONFIRMED').flatMap(a => a.evidence.opportunityValues);
  const unknown = assessments.filter(a => a.outcome === 'UNKNOWN' || a.outcome === 'NOT_ASSESSED');
  return <div data-comparison-for={state.result.opportunityId}>
    <p className="comparison-summary" role="status">{t(summary[state.result.evaluation.constraintSummary])}</p>
    <ul className="fit-list">
      {shared.length > 0 && <li>{t("Technologies in common:")} {shared.join(', ')}.</li>}
      {known.map(a => <Explanation key={a.criterion} assessment={a} />)}
    </ul>
    <details><summary>{t("What’s still unknown or not assessed")}</summary>
      <ul className="fit-list">{unknown.map(a => <Explanation key={a.criterion} assessment={a} />)}</ul>
      <p>{t(job.unknown)}</p>
    </details>
    <p className="field-hint">{t("Based on your saved profile. These checks do not establish suitability or work authorization.")}</p>
  </div>;
}

export function ProfileComparison({ job, profile }: { job: Opportunity; profile: ProfessionalProfile | null }) {
  const { t } = useLanguage();
  return <section className="fit-section" aria-labelledby="comparison-title">
    <p className="eyebrow">{t("A CLOSER LOOK")}</p><h2 id="comparison-title">{t("How it lines up")}</h2>
    {profile ? <SavedComparison key={job.id + ':' + JSON.stringify(profile)} job={job} profile={profile} />
      : <p><a href="#profile">{t("Set up your profile")}</a> {t("to compare your preferences with the listed requirements.")}</p>}
  </section>;
}
