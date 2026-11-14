import { reasonSpanish } from '../i18n/reasons';
import { useLanguage } from '../i18n/Language';
import type { Assessment, Comparison } from '../api/matching';

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

function Explanation({ assessment, live }: { assessment: Assessment; live?: boolean }) {
  const { t, language } = useLanguage();
  const name = assessment.dimension === 'TECHNOLOGY'
    ? assessment.evidence.opportunityValues.join(', ') || t('Technologies') : t(labels[assessment.dimension]);
  let explanation = language === 'es' ? reasonSpanish[assessment.reason] : assessment.explanation;
  if (live && (assessment.reason === 'ATTENDANCE_EXCEEDS_LIMIT' || assessment.reason === 'ATTENDANCE_WITHIN_LIMIT')) {
    const [days] = assessment.evidence.opportunityValues;
    const [max] = assessment.evidence.profileValues;
    if (/^[0-5]$/.test(days) && /^[0-5]$/.test(max)) {
      explanation = language === 'es'
        ? `La oferta exige ${days} ${days === '1' ? 'día presencial' : 'días presenciales'} por semana; tu máximo es ${max}.`
        : `The listing requires ${days} office ${days === '1' ? 'day' : 'days'} per week; your maximum is ${max}.`;
    }
  }
  return <li data-criterion={assessment.criterion} data-outcome={assessment.outcome}>
    <strong>{name}</strong> — {explanation}
  </li>;
}

export function ComparisonResult({ result, catalogNote }: { result: Comparison; catalogNote?: string }) {
  const { t } = useLanguage();
  const live = catalogNote === undefined;
  const assessments = result.evaluation.assessments;
  const known = assessments.filter(a => a.dimension !== 'TECHNOLOGY' && (a.outcome === 'ALIGNED' || a.outcome === 'DIFFERENT'));
  const shared = assessments.filter(a => a.reason === 'TECHNOLOGY_CONFIRMED').flatMap(a => a.evidence.opportunityValues);
  const unknown = assessments.filter(a => a.outcome === 'UNKNOWN' || a.outcome === 'NOT_ASSESSED');
  const attendance = live ? unknown.find(a => a.dimension === 'ATTENDANCE') : undefined;
  return <div data-comparison-for={result.opportunityId}>
    <p className="comparison-summary" role="status">{t(summary[result.evaluation.constraintSummary])}</p>
    <ul className="fit-list">
      {shared.length > 0 && <li>{t("Technologies in common:")} {shared.join(', ')}.</li>}
      {known.map(a => <Explanation key={a.criterion} assessment={a} live={live} />)}
      {attendance && <Explanation assessment={attendance} live />}
    </ul>
    <details><summary>{t("What’s still unknown or not assessed")}</summary>
      <ul className="fit-list">{unknown.filter(a => a !== attendance).map(a => <Explanation key={a.criterion} assessment={a} />)}</ul>
      {catalogNote && <p>{t(catalogNote)}</p>}
    </details>
    <p className="field-hint">{t("Based on your saved profile. These checks do not establish suitability or work authorization.")}</p>
  </div>;
}

