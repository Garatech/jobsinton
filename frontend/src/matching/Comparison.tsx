import { ComparisonResult } from './ComparisonResult';
import { useLanguage } from '../i18n/Language';
import { useEffect, useState } from 'react';
import { ComparisonError, fetchComparison, type Comparison as Result } from '../api/matching';
import type { Opportunity } from '../catalog/data';
import type { ProfessionalProfile } from '../profile/model';

type State = { kind: 'loading' } | { kind: 'ready'; result: Result } | { kind: 'error'; message: string };
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
  return <ComparisonResult result={state.result} catalogNote={job.unknown} />;
}

export function ProfileComparison({ job, profile }: { job: Opportunity; profile: ProfessionalProfile | null }) {
  const { t } = useLanguage();
  return <section className="fit-section" aria-labelledby="comparison-title">
    <p className="eyebrow">{t("A CLOSER LOOK")}</p><h2 id="comparison-title">{t("How it lines up")}</h2>
    {profile ? <SavedComparison key={job.id + ':' + JSON.stringify(profile)} job={job} profile={profile} />
      : <p><a href="#profile">{t("Set up your profile")}</a> {t("to compare your preferences with the listed requirements.")}</p>}
  </section>;
}
