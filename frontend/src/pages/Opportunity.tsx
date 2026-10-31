import { findCompany, opportunities, salaryLabel, type Opportunity as Job } from '../catalog/data';
import type { ProfessionalProfile } from '../profile/model';
import { ProfileComparison } from '../matching/Comparison';
import { ApplicationDialog, CompanyMark, JobList, SaveButton, Tags, type SavedActions } from '../ui';

export function OpportunityDetail({ job, profile, ...actions }: SavedActions & { job: Job; profile: ProfessionalProfile | null }) {
  const company = findCompany(job.company)!;
  return <>
    <nav className="breadcrumb" aria-label="Breadcrumb"><a href="#opportunities">Opportunities</a><span aria-hidden="true">/</span><span>{job.title}</span></nav>
    <section className="job-heading"><div className="company-lockup"><CompanyMark company={company} /><a href={'#companies/' + company.id}>{company.name} <span aria-hidden="true">↗</span></a></div>
      <h1>{job.title}<span className="brand-dot">.</span></h1><p className="detail-subtitle">{job.location} <span aria-hidden="true">·</span> {job.mode} <span aria-hidden="true">·</span> Full-time</p>
      <div className="job-heading-bottom"><Tags items={job.tags} /><span className="muted">Published <time dateTime={job.published}>{job.published}</time></span></div>
    </section>
    <div className="detail-layout"><article className="prose">
      <section><h2>The opportunity</h2><p>{job.intro}</p></section><section><h2>What you’ll work on</h2><ul>{job.tasks.map(x => <li key={x}>{x}</li>)}</ul></section>
      <section><h2>What you’ll bring</h2><ul>{job.requirements.map(x => <li key={x}>{x}</li>)}</ul></section><section><h2>Beyond the role</h2><ul>{company.benefits.map(x => <li key={x}>{x}</li>)}</ul></section>
      <ProfileComparison job={job} profile={profile} />
    </article><aside className="detail-sidebar"><div className="apply-card"><p className="eyebrow">THE ESSENTIALS</p><p className="large-salary">{salaryLabel(job)}</p><p className="muted">EUR gross per year</p>
      <dl><dt>Experience</dt><dd>{job.experience}</dd><dt>Contract</dt><dd>Permanent · Full-time</dd><dt>Work mode</dt><dd>{job.mode}{job.officeDays ? ' · ' + job.officeDays + ' office days / week' : ''}</dd></dl>
      <ApplicationDialog job={job} /><SaveButton job={job} {...actions} />
    </div><a className="company-mini" href={'#companies/' + company.id}><CompanyMark company={company} /><span><strong>{company.name}</strong><span>Meet the team <span aria-hidden="true">↗</span></span></span></a></aside></div>
    <section className="section"><div className="section-heading"><h2>Keep exploring.</h2><a className="text-link" href="#opportunities">All opportunities <span aria-hidden="true">↗</span></a></div><JobList jobs={opportunities.filter(j => j.id !== job.id).slice(0, 2)} {...actions} /></section>
  </>;
}
