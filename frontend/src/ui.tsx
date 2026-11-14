import { useLanguage } from './i18n/Language';
import { useRef, type ReactNode } from 'react';
import { findCompany, salaryLabel, type Company, type Opportunity } from './catalog/data';

export interface SavedActions { saved: string[]; onToggle: (id: string) => void; }
export function PageHeading({ eyebrow, title, description }: { eyebrow: string; title: string; description?: string }) {
  const { t } = useLanguage();
  return <header className="page-heading"><p className="eyebrow">{t(eyebrow)}</p><h1>{t(title)}</h1>{description && <p className="intro">{t(description)}</p>}</header>;
}
export function CompanyMark({ company }: { company: Company }) {
  return <span className={'company-mark ' + company.color} aria-hidden="true">{company.initials}</span>;
}
export function Tags({ items }: { items: string[] }) {
  const { t } = useLanguage();
  return <ul className="tags" aria-label={t("Technologies")}>{items.map(t => <li key={t}>{t}</li>)}</ul>;
}
export function SaveButton({ job, saved, onToggle }: SavedActions & { job: Opportunity }) {
  const { t } = useLanguage();
  const isSaved = saved.includes(job.id);
  return <button className="save-button secondary-button" type="button" data-save={job.id} aria-pressed={isSaved}
    aria-label={t(isSaved ? 'Remove {name} from saved' : 'Save {name}').replace('{name}', () => t(job.title))}
    onClick={() => onToggle(job.id)}>{t(isSaved ? 'Saved ✓' : 'Save ＋')}</button>;
}
export function JobCard({ job, comparisonUnavailable, ...actions }: SavedActions & { job: Opportunity; comparisonUnavailable?: boolean }) {
  const { t, date, language, euros } = useLanguage();
  const company = findCompany(job.company)!;
  return <article className="job" data-job={job.id}>
    <div className="job-main"><CompanyMark company={company} /><div>
      <p className="company-name"><a href={'#companies/' + company.id}>{company.name}</a></p>
      <h3><a href={'#opportunities/' + job.id}>{t(job.title)}</a></h3>
      <p className="job-meta">{t(job.location)}<span aria-hidden="true"> · </span>{t(job.mode)}</p>
      {comparisonUnavailable && <p className="job-meta">{t("Comparison unavailable")}</p>}
      <p className="job-line">{t(job.line)}</p><Tags items={job.tags} />
    </div></div>
    <div className="job-end"><p className="salary">{language === 'en' ? salaryLabel(job) : euros(job.salary) + '–' + euros(job.max)}<span>{t("gross / year")}</span></p>
      <p className="posted">{t("Published")} <time dateTime={job.published}>{date(job.published)}</time></p>
      <SaveButton job={job} {...actions} />
    </div>
  </article>;
}
export function JobList({ jobs, className = '', unavailableIds, ...actions }: SavedActions & { jobs: Opportunity[]; className?: string; unavailableIds?: string[] }) {
  return <div className={'jobs ' + className}>{jobs.map(job => <JobCard key={job.id} job={job} comparisonUnavailable={unavailableIds?.includes(job.id)} {...actions} />)}</div>;
}
export function EmptyState({ title, description, children }: { title: string; description: string; children: ReactNode }) {
  const { t } = useLanguage();
  return <div className="empty-state"><span className="empty-symbol" aria-hidden="true">↗</span><h2>{t(title)}</h2><p>{t(description)}</p>{children}</div>;
}
export function ApplicationDialog({ job }: { job: Opportunity }) {
  const { t } = useLanguage();
  const ref = useRef<HTMLDialogElement>(null);
  const company = findCompany(job.company)!;
  return <>
    <button className="primary" type="button" onClick={() => ref.current?.showModal()}>{t("Apply for this role")} <span aria-hidden="true">↗</span></button>
    <dialog ref={ref} aria-labelledby="application-title"><p className="eyebrow">{company.name}</p><h2 id="application-title">{t("Your next step.")}</h2>
      <p>{t("This catalog contains fictional opportunities for exploring JobSinton.")}</p>
      <div className="application-summary"><strong>{t(job.title)}</strong><span>{t(job.location)} · {t(job.mode)}</span></div>
      <p className="field-hint">{t("No application will be sent. Live applications will become available with a job provider.")}</p>
      <button className="primary" type="button" onClick={() => ref.current?.close()}>{t("Back to opportunity")}</button>
    </dialog>
  </>;
}
