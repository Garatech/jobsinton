import { useRef, type ReactNode } from 'react';
import { findCompany, salaryLabel, type Company, type Opportunity } from './catalog/data';

export interface SavedActions { saved: string[]; onToggle: (id: string) => void; }
export function PageHeading({ eyebrow, title, description }: { eyebrow: string; title: string; description?: string }) {
  return <header className="page-heading"><p className="eyebrow">{eyebrow}</p><h1>{title}</h1>{description && <p className="intro">{description}</p>}</header>;
}
export function CompanyMark({ company }: { company: Company }) {
  return <span className={'company-mark ' + company.color} aria-hidden="true">{company.initials}</span>;
}
export function Tags({ items }: { items: string[] }) {
  return <ul className="tags" aria-label="Technologies">{items.map(t => <li key={t}>{t}</li>)}</ul>;
}
export function SaveButton({ job, saved, onToggle }: SavedActions & { job: Opportunity }) {
  const isSaved = saved.includes(job.id);
  return <button className="save-button secondary-button" type="button" data-save={job.id} aria-pressed={isSaved}
    aria-label={(isSaved ? 'Remove ' : 'Save ') + job.title + (isSaved ? ' from saved' : '')}
    onClick={() => onToggle(job.id)}>{isSaved ? 'Saved ✓' : 'Save ＋'}</button>;
}
export function JobCard({ job, ...actions }: SavedActions & { job: Opportunity }) {
  const company = findCompany(job.company)!;
  return <article className="job" data-job={job.id}>
    <div className="job-main"><CompanyMark company={company} /><div>
      <p className="company-name"><a href={'#companies/' + company.id}>{company.name}</a></p>
      <h3><a href={'#opportunities/' + job.id}>{job.title}</a></h3>
      <p className="job-meta">{job.location}<span aria-hidden="true"> · </span>{job.mode}</p>
      <p className="job-line">{job.line}</p><Tags items={job.tags} />
    </div></div>
    <div className="job-end"><p className="salary">{salaryLabel(job)}<span>gross / year</span></p>
      <p className="posted">Published <time dateTime={job.published}>{job.published}</time></p>
      <SaveButton job={job} {...actions} />
    </div>
  </article>;
}
export function JobList({ jobs, className = '', ...actions }: SavedActions & { jobs: Opportunity[]; className?: string }) {
  return <div className={'jobs ' + className}>{jobs.map(job => <JobCard key={job.id} job={job} {...actions} />)}</div>;
}
export function EmptyState({ title, description, children }: { title: string; description: string; children: ReactNode }) {
  return <div className="empty-state"><span className="empty-symbol" aria-hidden="true">↗</span><h2>{title}</h2><p>{description}</p>{children}</div>;
}
export function ApplicationDialog({ job }: { job: Opportunity }) {
  const ref = useRef<HTMLDialogElement>(null);
  const company = findCompany(job.company)!;
  return <>
    <button className="primary" type="button" onClick={() => ref.current?.showModal()}>Apply for this role <span aria-hidden="true">↗</span></button>
    <dialog ref={ref} aria-labelledby="application-title"><p className="eyebrow">{company.name}</p><h2 id="application-title">Your next step.</h2>
      <p>This catalog contains fictional opportunities for exploring JobSinton.</p>
      <div className="application-summary"><strong>{job.title}</strong><span>{job.location} · {job.mode}</span></div>
      <p className="field-hint">No application will be sent. Live applications will become available with a job provider.</p>
      <button className="primary" type="button" onClick={() => ref.current?.close()}>Back to opportunity</button>
    </dialog>
  </>;
}
